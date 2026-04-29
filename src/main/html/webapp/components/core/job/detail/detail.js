import $ from "jquery";
import Control from "can-control";
import bootbox from "bootbox";
import canRoute from "can-route";

import ErrorPage from "helpers/error-page";
import showErrorDialog from "helpers/error-dialog";

import Job from "models/job";
import JobDetails from "models/job-details";
import JobOperation from "models/job-operation";

import ResultsControl from "./results/";
import StepsControl from "./steps/";
import LogsControl from "./logs/";
import WebpageControl from "./webpage/";

import template from "./detail.stache";

export default Control.extend({
  init: function (element, options) {
    var that = this;
    this.active = true;
    console.log("Start INIT");
    console.log("Options passed to control:", options);

    if (!options.tab) {
      options.tab = "steps";
    }
    console.log("Selected tab:", options.tab);
    console.log("Options.job:", options.job);
    console.log(options.job);

    JobDetails.findOne(
      { id: options.job },
      function (job) {
        console.log(job);
        try {
          console.log("Job details loaded:", job);
          console.log("Job outputParams:", job.attr('outputParams') ? job.attr('outputParams').serialize() : 'none');
          console.log("Has webpage output:", job.attr('hasWebpageOutput'));
          console.log("Webpage output params:", job.attr('webpageOutputParams'));
          $(element).html(
            template({
              job: job,
              tab: options.tab,
              admin:
                options.appState && options.appState.attr("user")
                  ? options.appState.attr("user").attr("admin")
                  : false,
            }),
          );

          switch (options.tab) {
            case "results":
              console.log("Initializing ResultsControl");
              new ResultsControl("#tab-results", { job: job });
              break;

            case "steps":
              console.log("Initializing StepsControl");
              new StepsControl("#tab-steps", { job: job });
              break;

            case "logs":
              console.log("Initializing LogsControl");
              new LogsControl("#tab-logs", { job: job });
              break;

            case "webpage":
              console.log("Initializing WebpageControl");
              new WebpageControl("#tab-webpage", { job: job });
              break;

            default:
              console.log("Unknown tab selected:", options.tab);
          }

          $('[data-toggle="tooltip"]').tooltip();

          that.job = job;
          console.log("Job instance assigned to control:", that.job);
          that.refresh();
        } catch (error) {
          console.error("Error during job initialization:", error);
          showErrorDialog(
            "An error occurred while initializing the job details.",
            error,
          );
        }
      },
      function (response) {
        console.error("Error loading job details:", response);
        console.log("Attempting to render error page...");
        new ErrorPage(that.element, response);
      },
    );
  },

  // delete job

  "#delete-btn click": function (el, ev) {
    var that = this;

    bootbox.confirm(
      "Are you sure you want to delete <b>" + that.job.attr("name") + "</b>?",
      function (result) {
        if (result) {
          var okButton = $("button[data-bb-handler='confirm']");
          okButton.prop("disabled", true);
          okButton.html("Please wait...");
          var cancelButton = $("button[data-bb-handler='cancel']");
          cancelButton.hide("hide");

          that.job.destroy(
            function () {
              // go to jobs page
              bootbox.hideAll();
              window.location.hash = "!pages/home";
            },
            function (response) {
              bootbox.hideAll();
              showErrorDialog("Job could not be deleted", response);
            },
          );

          return false;
        }
      },
    );
  },

  // cancel job

  "#cancel-btn click": function (el, ev) {
    var that = this;

    bootbox.confirm(
      "Are you sure you want to cancel <b>" + that.job.attr("name") + "</b>?",
      function (result) {
        if (result) {
          var okButton = $("button[data-bb-handler='confirm']");
          okButton.prop("disabled", true);
          okButton.html("Please wait...");
          var cancelButton = $("button[data-bb-handler='cancel']");
          cancelButton.hide("hide");

          var operation = new JobOperation();
          operation.attr("id", that.job.attr("id"));
          operation.attr("action", "cancel");
          operation.save(
            function () {
              bootbox.hideAll();
              that.refresh();
            },
            function (response) {
              bootbox.hideAll();
              showErrorDialog("Job could not be canceld", response);
            },
          );

          return false;
        }
      },
    );
  },

  "#restart-btn click": function (el, ev) {
    var that = this;

    bootbox.confirm(
      "Are you sure you want to restart <b>" + that.job.attr("name") + "</b>?",
      function (result) {
        if (result) {
          var okButton = $("button[data-bb-handler='confirm']");
          okButton.prop("disabled", true);
          okButton.html("Please wait...");
          var cancelButton = $("button[data-bb-handler='cancel']");
          cancelButton.hide("hide");

          var operation = new JobOperation();
          operation.attr("id", that.job.attr("id"));
          operation.attr("action", "restart");
          operation.save(
            function () {
              bootbox.hideAll();
              window.location.hash = "#!pages/jobs";
            },
            function (response) {
              bootbox.hideAll();
              showErrorDialog("Job could not be restarted", response);
            },
          );

          return false;
        }
      },
    );
  },

  // refresh if job is running

  refresh: function () {
    var that = this;
    if (!JobRefresher.needsUpdate(that.job)) {
      return;
    }
    Job.findOne(
      {
        id: that.job.id,
      },
      function (currentJob) {
        currentJob.syncTime();
        that.job.attr("state", currentJob.attr("state"));
        that.job.attr("startTime", currentJob.attr("startTime"));
        that.job.attr("endTime", currentJob.attr("endTime"));
        that.job.attr("steps", currentJob.attr("steps"));
        that.job.attr("positionInQueue", currentJob.attr("positionInQueue"));

        // needs refresh
        if (JobRefresher.needsUpdate(currentJob) && that.active) {
          setTimeout(function () {
            that.refresh();
          }, 20000);
        } else {
          // updates details (results, startTime, endTime, ...)
          JobDetails.findOne(
            {
              id: that.job.id,
            },
            function (job) {
              if (that.active) {
                var router = canRoute.router;
                router.reload();
              }
            },
            function (response) {
              new ErrorPage(that.element, response);
            },
          );
        }
      },
    );
  },

  destroy: function () {
    this.active = false;
    Control.prototype.destroy.call(this);
  },
});

var JobRefresher = {};

JobRefresher.needsUpdate = function (job) {
  return (
    job.attr("state") == 1 || job.attr("state") == 2 || job.attr("state") == 3
  );
};
