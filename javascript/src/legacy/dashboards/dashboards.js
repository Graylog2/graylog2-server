var dashboard = $(".dashboard");
var toggleDashboardLock = $("#toggle-dashboard-lock");
var unlockDashboardLink = $("#unlock-dashboard");
var dragWidgetsDescription = $("#drag-widgets-description");
var toggleUpdateUnfocussed = $("#update-unfocussed");
var toggleFullscreen = $(".toggle-fullscreen");
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

function updateWidgetPositions() {
  var positions = dashboardGrid.serialize();
  var dashboardId = $(".gridster").attr("data-dashboard-id");

  var payload = {
    positions: positions
  };

  $.ajax({
    url: appPrefixed('/a/dashboards/' + dashboardId + '/positions'),
    type: 'POST',
    data: JSON.stringify(payload),
    processData: false,
    contentType: 'application/json',
    success: function (data) {
      // not doing anything here for now. no need to notify user about success IMO
    },
    error: function (data) {
      showError("Could not save widget positions.");
    }
  });
}

export const initializeDashboard = function() {
  /* ducksboard/gridster.js#147 Hotfix - Part 1 */
  var items = $(".gridster ul li");
  items.detach();

  dashboardGrid = $(".gridster ul").gridster({
    widget_margins: [10, 10],
    widget_base_dimensions: [410, 200],
    resize: {
      enabled: true,
      stop: function() {
        updateWidgetPositions();
      }
    },
    draggable: {
      stop: function() {
        updateWidgetPositions();
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
  dashboardGrid.disable();
  dashboardGrid.disable_resize();


  /* ducksboard/gridster.js#147 Hotfix - Part 2 */
  $.each(items , function (i, e) {
    var item = $(this);
    var columns = parseInt(item.attr("data-sizex"));
    var rows = parseInt(item.attr("data-sizey"));
    var col = parseInt(item.attr("data-col"));
    var row = parseInt(item.attr("data-row"));
    dashboardGrid.add_widget(item, columns, rows, col, row);
  });
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

function hideDashboardControls() {
  "use strict";
  toggleUpdateUnfocussed.hide();
  toggleFullscreen.hide();
  toggleDashboardLock.hide();
  dragWidgetsDescription.hide();
}

function showEmptyDashboardMessage() {
  "use strict";
  var $parent = dashboard.parent();
  var $emptyDashboardAlert = $("<div/>", {
    class: "alert alert-info no-widgets",
    text: "No more widgets to display"
  });
  var $emptyDashboardHelper = $("<div/>", { class: "content col-md-12" }).append($emptyDashboardAlert);
  $parent.prepend($emptyDashboardHelper);
}

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

const updateInBackground = function() {
  setUpdateUnfocussedMode(true);
  alert("Graphs will be updated even when the browser is in the background");
};

const updateInFocus = function() {
  setUpdateUnfocussedMode(false);
  alert("Graphs will be updated only when the browser is in the foreground");
};

const unlockDashboard = function() {
  dashboardGrid.enable();
  dashboardGrid.enable_resize();
  dashboard.addClass("unlocked");
  $(this).hide();
  $(".only-unlocked").show();
  $(".hidden-unlocked").hide();

  $(".dashboard .widget").each(function() {
    $(this).trigger("unlocked.graylog.dashboard");
  });

  toggleDashboardLock.text("Lock");
  toggleDashboardLock.data('locked', false);
};

const lockDashboard = function() {
  dashboardGrid.disable();
  dashboardGrid.disable_resize();
  dashboard.removeClass("unlocked");
  $(this).hide();
  $(".hidden-unlocked").show();
  $(".only-unlocked").hide();

  $(".dashboard .widget").each(function() {
    $(this).trigger("locked.graylog.dashboard");
  });

  toggleDashboardLock.text("Unlock / Edit");
  toggleDashboardLock.data('locked', true);
};

function isDashboardLocked() {
  "use strict";
  return Boolean(toggleDashboardLock.data('locked'));
}

