$(document).ready(function() {
    var dashboard = $(".dashboard");
    var toggleDashboardLock = $("#toggle-dashboard-lock");
    var unlockDashboardLink = $("#unlock-dashboard");
    var toggleUpdateUnfocussed = $("#update-unfocussed");

    // Load all writable dashboards in the global registry first.
    $.ajax({
        url: appPrefixed('/a/dashboards/writable'),
        success: function(data) {
            globalDashboards = data;
            applyDashboardsToAllSelectors();
        }
    });

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

    var initializeDashboard = function() {
        /* ducksboard/gridster.js#147 Hotfix - Part 1 */
        var items = $(".gridster ul li");
        items.detach();

        dashboardGrid = $(".gridster ul").gridster({
            widget_margins: [10, 10],
            widget_base_dimensions: [410, 170],
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

    var reloadDashboard = function() {
        lockDashboard();
        dashboardGrid.destroy();
        initializeDashboard();
    };

    if (dashboard.length > 0){
        initializeDashboard();

        var resizeTimeout;
        $(window).on("resize", function(e) {
            // ignore resize events as long as a reloadDashboard execution is in the queue
            if (!resizeTimeout) {
                resizeTimeout = setTimeout(function() {
                    resizeTimeout = null;
                    reloadDashboard();
                }, 200);
            }
        });
    } else {
        toggleDashboardLock.hide();
    }

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

    function configuration(widgetType, element, callback) {
        var funcName = "configureDialog_" + widgetType;
        var func = window[funcName];
        if (func) {
            func(callback, element);
        } else {
            var description = prompt("Give the widget a title:");
            if (description != null && description != "") {
                callback(description);
            }
        }
    }

    $(document).on("click", "ul.dashboard-selector li a[data-dashboard-id]", function () {
        var elem = $(this).closest("ul.dashboard-selector");
        var widgetType = elem.attr("data-widget-type");
        var dashboardId = $(this).attr("data-dashboard-id");
        configuration(widgetType, elem, createDashboard);
        function createDashboard(description) {
            delegateAddToDashboard(
                widgetType,
                dashboardId,
                description,
                elem
            );
        }
    });

    if (dashboard.length > 0) {
        dashboard.on("delete.widget", function (event, details) {
            var dashboardId = $(".dashboard .gridster").data("dashboard-id");
            var widgetId = details.widgetId;
            var widget = $("[data-widget-id=" + widgetId + "]");
            var gridsterWidget = widget.parent("li");
            $.ajax({
                url: appPrefixed('/a/dashboards/' + dashboardId + '/widgets/' + widgetId + '/delete'),
                type: 'POST',
                success: function() {
                    showSuccess("Widget has been removed from dashboard!");
                    dashboardGrid.remove_widget(gridsterWidget);
                },
                error: function(data) {
                    showError("Could not remove widget from dashboard.");
                }
            });
        });
    }

    function delegateAddToDashboard(widgetType, dashboardId, description, elem) {
        var funcName = "addWidget_" + widgetType;
        window[funcName](dashboardId, description, elem);
    }

    var updateInBackground = function() {
        setUpdateUnfocussedMode(true);
        alert("Graphs will be updated even when the browser is in the background");
    };

    var updateInFocus = function() {
        setUpdateUnfocussedMode(false);
        alert("Graphs will be updated only when the browser is in the foreground");
    };

    toggleUpdateUnfocussed.on('click', function() {
        var updateUnfocussed = Boolean(toggleUpdateUnfocussed.data('update-unfocussed'));
        if(updateUnfocussed) {
            updateInFocus();
            toggleUpdateUnfocussed.text("Update in background");
        } else {
            updateInBackground();
            toggleUpdateUnfocussed.text("Update in foreground");
        }

        toggleUpdateUnfocussed.data('update-unfocussed', !updateUnfocussed);
    });

    var unlockDashboard = function() {
        dashboardGrid.enable();
        dashboardGrid.enable_resize();
        dashboard.addClass("unlocked");
        $(this).hide();
        $(".only-unlocked").show();
        $(".hidden-unlocked").hide();

        $(".dashboard .widget").each(function() {
            $(this).trigger("unlocked.dashboard");
        });

        toggleDashboardLock.text("Lock");
        toggleDashboardLock.data('locked', false);
    };

    var lockDashboard = function() {
        dashboardGrid.disable();
        dashboardGrid.disable_resize();
        dashboard.removeClass("unlocked");
        $(this).hide();
        $(".hidden-unlocked").show();
        $(".only-unlocked").hide();

        $(".dashboard .widget").each(function() {
            $(this).trigger("locked.dashboard");
        });

        toggleDashboardLock.text("Unlock / Edit");
        toggleDashboardLock.data('locked', true);
    };

    toggleDashboardLock.on('click', function() {
        var locked = Boolean(toggleDashboardLock.data('locked'));
        if (locked) {
            unlockDashboard();
        } else {
            lockDashboard();
        }
    });

    unlockDashboardLink.on('click', function() {
        unlockDashboard();
    });

    if (dashboard.length > 0) {
        lockDashboard();
    }
});

function addWidget(dashboardId, description, params) {
    if (description !== undefined && description !== "") {
        params.description = description;
    }

    $.ajax({
        url: appPrefixed('/a/dashboards/' + dashboardId + '/widgets'),
        type: 'POST',
        data: params,
        success: function() {
            showSuccess("Widget added to dashboard!")
        },
        error: function(data) {
            showError("Could not add widget to dashboard.");
        }
    });
}