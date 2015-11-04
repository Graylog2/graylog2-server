const dashboard = $(".dashboard");
const toggleDashboardLock = $("#toggle-dashboard-lock");
let dashboardGrid;

// Load all writable dashboards in the global registry first.
/*$.ajax({
 url: appPrefixed('/a/dashboards/writable'),
 success: function(data) {
 globalDashboards = data;
 applyDashboardsToAllSelectors();
 }
 });*/

require('!script!../../../public/javascripts/jquery-2.1.1.min.js');
require('!script!../../../public/javascripts/jquery.gridster.min.js');

export const initializeDashboard = function(updateWidgetCallback) {
  /* ducksboard/gridster.js#147 Hotfix - Part 1 */
  var items = $(".gridster ul li");
  items.detach();

  dashboardGrid = $(".gridster ul").gridster({
    widget_margins: [10, 10],
    widget_base_dimensions: [410, 200],
    resize: {
      enabled: true,
      stop: function() {
        updateWidgetCallback(dashboardGrid);
      }
    },
    draggable: {
      stop: function() {
        updateWidgetCallback(dashboardGrid);
      }
    },
    serialize_params: function(widgetListItem, pos) {
      var widget = $(".widget", widgetListItem);

      return {
        id: widget.attr("data-widget-id"),
        col: pos.col,
        row: pos.row,
        size_x: pos.size_x,
        size_y: pos.size_y
      }
    }
  }).data('gridster');

  if (dashboardGrid) {
    lockDashboard();
  }

  /* ducksboard/gridster.js#147 Hotfix - Part 2 */
  $.each(items , function (i, e) {
    var item = $(this);
    var columns = parseInt(item.attr("data-sizex"));
    var rows = parseInt(item.attr("data-sizey"));
    var col = parseInt(item.attr("data-col"));
    var row = parseInt(item.attr("data-row"));
    dashboardGrid.add_widget(item, columns, rows, col, row);
  });

  return dashboardGrid;
};

const reloadDashboard = function() {
  var initialDashboardLockedStatus = isDashboardLocked();
  if (!initialDashboardLockedStatus) {
    lockDashboard();
  }

  dashboardGrid.destroy();
  initializeDashboard();

  if (!initialDashboardLockedStatus) {
    unlockDashboard();
  }
};

/*var resizeTimeout;
 $(window).on("resize", function(e) {
 // ignore resize events as long as a reloadDashboard execution is in the queue
 if (!resizeTimeout) {
 resizeTimeout = setTimeout(function() {
 resizeTimeout = null;
 reloadDashboard();
 }, 200);
 }
 });*/

function applyDashboardsToAllSelectors() {
  var dashboardSelectors = $(".dashboard-selector[data-widget-type]");
  if (Object.keys(globalDashboards).length > 0) {
    dashboardSelectors.each(function() {
      var dashboardList = $(this);
      $("li", dashboardList).remove();

      for (var key in globalDashboards) {
        var dashboard = globalDashboards[key];
        var link = "<li><a href='#' data-dashboard-id='" + key + "'>" + htmlEscape(dashboard.title) + "</a></li>";
        dashboardList.append(link);
      }
    });
  }

  // Show helper message if there are no options in the dropdown
  dashboardSelectors.each(function() {
    var dashboardSelector = $(this);
    if (dashboardSelector.children().length === 0) {
      var link = "<li><a href='#'>No dashboard with enough permissions to add widgets.</a></li>";
      dashboardSelector.append(link);
    }
  });
}

export const unlockDashboard = function() {
  dashboardGrid.enable();
  dashboardGrid.enable_resize();
};

export const lockDashboard = function() {
  dashboardGrid.disable();
  dashboardGrid.disable_resize();
};

function isDashboardLocked() {
  "use strict";
  return Boolean(toggleDashboardLock.data('locked'));
}

