import Control from 'can-control';
import domData from 'can-util/dom/data/data';
import canMap from 'can-map';
import canRoute from 'can-route';

import 'helpers/helpers';
import $ from 'jquery';
import bootbox from 'bootbox';
import showErrorDialog from 'helpers/error-dialog';

import Application from 'models/application';
import Group from 'models/group';

import template from './list.stache';
import templateInstallGithub from './install-github/install-github.stache';
import templateInstallUrl from './install-url/install-url.stache';
import templatePermission from './permission/permission.stache';

export default Control.extend({

  "init": function (element, options) {
    var that = this;

    Application.findAll({}, function (applications) {

      var grouped = {};
      applications.forEach(function(item) {
        var category = item.category;
        if (category == undefined) {
          category = "Application";
        }
        if (!grouped[category]) {
          grouped[category] = [];
        }
        grouped[category].push(item);
      });

      var categories = Object.keys(grouped).map(category => {
        return {
          name: category,
          applications: grouped[category].map(app => {
            return app;
          })
        };
      });

      // Sort the categories array
      categories.sort(function(a, b) {
        // Place "Application" at the beginning
        if (a.name.toLowerCase() === "application") return -1;
        if (b.name.toLowerCase() === "application") return 1;

        // Compare other categories alphabetically
        return a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
      });


      $(element).html(template({
        categories: categories
      }));
      $(element).fadeIn();

    });

  },

  '#install-app-url-btn click': function (el, ev) {

    bootbox.confirm(templateInstallUrl(),
      function (result) {
        if (result) {
          var url = $('#url').val();
          var app = new Application();
          app.attr('url', url);

          var waitingDialog = bootbox.dialog({
            message: '<h4>Install application</h4>' +
              '<p>Please wait while the application is configured.</p>' +
              '<div class="progress progress-striped active">' +
              '<div id="waiting-progress" class="bar" style="width: 100%;"></div>' +
              '</div>',
            show: false
          });
          waitingDialog.on('shown.bs.modal', function () {
            app.save(function (application) {
              waitingDialog.modal('hide');
              bootbox.alert('<h4>Congratulations</h4><p>The application installation was successful.</p>', function () {
                var router = canRoute.router;
                router.reload();
              });

            }, function (response) {
              waitingDialog.modal('hide');
              showErrorDialog("Operation failed", response);
            });
          });

          waitingDialog.modal('show');
        }
      });
  },

  '#install-app-github-btn click': function (el, ev) {

    bootbox.confirm(templateInstallGithub(),
      function (result) {
        if (result) {

          var url = 'github://' + $('#url').val();
          var app = new Application();
          app.attr('url', url);

          var waitingDialog = bootbox.dialog({
            message: '<h4>Install application</h4>' +
              '<p>Please wait while the application is configured.</p>' +
              '<div class="progress progress-striped active">' +
              '<div id="waiting-progress" class="bar" style="width: 100%;"></div>' +
              '</div>',
            show: false
          });

          waitingDialog.on('shown.bs.modal', function () {

            app.save(function (application) {
              waitingDialog.modal('hide');
              bootbox.alert('<h4>Congratulations</h4><p>The application installation was successful.</p>', function () {
                var router = canRoute.router;
                router.reload();
              });

            }, function (response) {
              waitingDialog.modal('hide');
              showErrorDialog("Operation failed", response);
            });
          });

          waitingDialog.modal('show');
        }
      });

  },

  '#reload-apps-btn click': function (el, ev) {
    var element = this.element;

    Application.findAll({
      reload: 'true'
    }, function (applications) {

      var grouped = {};
      applications.forEach(function(item) {
        var category = item.category;
        if (category == undfined) {
          category = "Application";
        }
        if (!grouped[category]) {
          grouped[category] = [];
        }
        grouped[category].push(item);
      });

      var categories = Object.keys(grouped).map(category => {
        return {
          name: category,
          applications: grouped[category].map(app => {
            return app;
          })
        };
      });

     // Sort the categories array
      categories.sort(function(a, b) {
        // Place "Application" at the beginning
        if (a.name.toLowerCase() === "application") return -1;
        if (b.name.toLowerCase() === "application") return 1;

        // Compare other categories alphabetically
        return a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
      });


      $(element).html(template({
        categories: categories
      }));
      $("#content").fadeIn();

    });
  },

  '.enable-disable-btn click': function (el, ev) {
    var card = $(el).closest('tr');
    var application = domData.get.call(card[0], 'application');

    var enabled = !application.attr('enabled')
    bootbox.confirm("Are you sure you want to " + (enabled ? "enable" : "disable") + " application <b>" + application.attr('id') + "</b>?", function (result) {
      if (result) {
        application.attr('enabled', enabled);

        var waitingDialog = bootbox.dialog({
          message: (enabled ? '<h4>Enable application</h4>' : '<h4>Disable application</h4>') +
            '<p>Please wait while the application is configured.</p>' +
            '<div class="progress progress-striped active">' +
            '<div id="waiting-progress" class="bar" style="width: 100%;"></div>' +
            '</div>',
          show: false
        });
        waitingDialog.on('shown.bs.modal', function () {

          application.save(function (application) {
            waitingDialog.modal('hide');
            bootbox.alert('<h4>Congratulations</h4><p>The application has been successfully ' + (enabled ? 'enabled' : 'disabled') + '.</p>');

          }, function (response) {
            waitingDialog.modal('hide');
            showErrorDialog("Operation failed", response);
          });
        });
        waitingDialog.modal('show');

      }
    });
  },

  '.delete-app-btn click': function (el, ev) {

    var card = $(el).closest('tr');
    var application = domData.get.call(card[0], 'application');

    bootbox.confirm("Are you sure you want to delete <b>" + application.attr('id') + "</b>?", function (result) {
      if (result) {

        var waitingDialog = bootbox.dialog({
          message: '<h4>Uninstall application</h4>' +
            '<p>Please wait while the application is configured.</p>' +
            '<div class="progress progress-striped active">' +
            '<div id="waiting-progress" class="bar" style="width: 100%;"></div>' +
            '</div>',
          show: false
        });

        waitingDialog.on('shown.bs.modal', function () {

          application.destroy(function (application) {
            waitingDialog.modal('hide');
            bootbox.alert('<h4>Congratulations</h4><p>The application has been successfully removed.</p>');

          }, function (response) {
            waitingDialog.modal('hide');
            showErrorDialog("Operation failed", response);
          });

        });

        waitingDialog.modal('show');
      }
    });

  },

  '.edit-permission-btn click': function(el, ev) {

    var card = $(el).closest('tr');
    var application = domData.get.call(card[0], 'application');

    Group.findAll({},
      function(groups) {

        var roles = application.attr('permission').split(',');

        var options = '';
        groups.forEach(function(group, index) {
          if ($.inArray(group.attr('name'), roles) >= 0) {
            options = options + '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" checked />';
            //options = options + '<option selected>' + group.attr('name') + '</option>';
          } else {
            //options = options + '<option>' + group.attr('name') + '</option>';
            options = options + '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" />';
          }
          options = options + ' <b>' + group.attr('name') + '</b></label><br>';
        });

      // Add input field for creating a new group
      var newGroupSection = `
        <hr>
        <label for="new-group-name">New Group Name:</label>
        <input type="text" id="new-group-name" class="form-control" placeholder="Enter new group name">
      `;

        bootbox.confirm(
          '<h4>Edit permission of ' + application.attr('name') + '</h4><hr><form id="role-form">' + options + newGroupSection + '</form>',
          function(result) {
            if (result) {

              var boxes = $('#role-form input:checkbox');
              var checked = [];
              for (var i = 0; boxes[i]; ++i) {
                if (boxes[i].checked) {
                  checked.push(boxes[i].value);
                }
              }

               var newGroupName = $('#new-group-name').val().trim();
              if (newGroupName) {
                checked.push(newGroupName);
              }

              var text = checked.join(',');
              application.attr('permission', text);
              application.save(function(data) {},
                function(response) {
                  showErrorDialog("Operation failed", response);
                });

            }
          }
        );

      },
      function(response) {
        new ErrorPage(element, response);
      });

  },

  '.view-source-btn click': function(el, ev) {

    var card = $(el).closest('tr');
    var application = domData.get.call(card[0], 'application');
    bootbox.alert({
      message: '<div style="overflow: auto; height: 600px; width: 100%"><h5>File</h5><p>' + application.attr('filename') + '</p>' + '<h5>Source</h5><small><p><pre><code>' + application.attr('source') + '</code></pre></small></p></div>',
      className: 'w-100'
    });
  }

});
