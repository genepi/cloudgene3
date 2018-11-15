import 'can-map-define';
import Control from 'can-control';
import domData from 'can-util/dom/data/data';
import $ from 'jquery';
import bootbox from 'bootbox';
import 'helpers/helpers';

import ErrorPage from 'helpers/error-page';
import 'helpers/helpers';
import JobAdminDetails from 'models/job-admin-details';
import JobOperation from 'models/job-operation';

import template from './list.stache';
import templateTable from './table/table.stache';

export default Control.extend({

  "init": function(element, options) {
    $(element).html(template());
    $(element).fadeIn();

    this.loadJobs("#job-list-running-stq", "running-stq");
    this.loadJobs("#job-list-running-ltq", "running-ltq");
    this.loadJobs("#job-list-current", "current");


  },

  loadJobs: function(element, mySate) {
    var that = this;
    JobAdminDetails.findAll({
      state: mySate
    }, function(jobs) {
      $.each(jobs, function(key, job) {
        job.syncTime();
      });
      $(element).html(templateTable({
        jobs: jobs
      }));
    }, function(response) {
      new ErrorPage(that.element, response);
    });

  },

  '.delete-btn click': function(el, ev) {
    var tr = $(el).closest('tr');
    var job = domData.get.call(tr[0], 'job');

    bootbox.confirm("Are you sure you want to delete <b>" + job.attr('id') + "</b>?", function(result) {
      if (result) {
        job.destroy();
      }
    });

  },

  '.cancel-btn click': function(el, ev) {

    var element = this.element;

    var tr = $(el).closest('tr');
    var job = domData.get.call(tr[0], 'job');

    bootbox.confirm("Are you sure you want to cancel <b>" + job.attr('id') + "</b>?", function(result) {
      if (result) {
        // cancel

        $("a[data-handler='1']").button('loading');
        $("a[data-handler='0']").hide('hide');

        var operation = new JobOperation();
        operation.attr('id', job.attr('id'));
        operation.attr('action', 'cancel');
        operation.save(function() {
          // go to jobs page
          bootbox.hideAll();
        }, function(response) {
          new ErrorPage(element, response);
        });

        return false;
      }
    });

  },

  '.priority-btn click': function(el, ev) {

    var tr = $(el).closest('tr');
    var job = domData.get.call(tr[0], 'job');
    var that = this;
    $.get('api/v2/admin/jobs/' + job.attr('id') + '/priority').then(
      function(data) {
        bootbox.alert(data);
        that.init(that.element, that.options);
      },
      function(data) {
        bootbox.alert('<p class="text-danger">Operation failed.</p>' + data.responseText);
      });
  },

  '.archive-btn click': function(el, ev) {
    var tr = $(el).closest('tr');
    var job = domData.get.call(tr[0], 'job');
    var that = this;

    bootbox.confirm("Are you sure you want to archive <b>" + job.attr('id') + "</b> now? <b>All results will be deleted!</b>", function(result) {
      if (result) {
        $.get('api/v2/admin/jobs/' + job.attr('id') + '/archive').then(
          function(data) {
            bootbox.alert(data);
            that.init(that.element, that.options);
          },
          function(data) {
            bootbox.alert('<p class="text-danger">Operation failed.</p>' + data.responseText);
          });
      }
    });
  },

  '.reset-downloads-btn click': function(el, ev) {
    var tr = $(el).closest('tr');
    var job = domData.get.call(tr[0], 'job');
    $.get('api/v2/admin/jobs/' + job.attr('id') + '/reset').then(
      function(data) {
        bootbox.alert(data);
      },
      function(data) {
        bootbox.alert('<p class="text-danger">Operation failed.</p>' + data.responseText);
      });
  },

  '.retire-btn click': function(el, ev) {
    var tr = $(el).closest('tr');
    var job = domData.get.call(tr[0], 'job');
    var that = this;
    $.get('api/v2/admin/jobs/' + job.attr('id') + '/retire').then(
      function(data) {
        bootbox.alert(data);
        that.init(that.element, that.options);
      },
      function(data) {
        bootbox.alert('<p class="text-danger">Operation failed.</p>' + data.responseText);
      });
  },

  '.change-retire-date-btn click': function(el, ev) {
    var tr = $(el).closest('tr');
    var job = domData.get.call(tr[0], 'job');
    var that = this;
    $.get('api/v2/admin/jobs/' + job.attr('id') + '/change-retire/1').then(
      function(data) {
        bootbox.alert(data);
        that.init(that.element, that.options);
      },
      function(data) {
        bootbox.alert('<p class="text-danger">Operation failed.</p>' + data.responseText);
      });
  }

});
