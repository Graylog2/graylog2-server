$(document).ready(function() {

    // Load all dashboards in the global registry first.
    $.ajax({
        url: '/a/dashboards',
        success: function(data) {
            globalDashboards = data;
            applyDashboardsToAllSelectors();
        },
        error: function(data) {
            showError("Could not load list of dashboards");
        }
    });

    $(".gridster ul").gridster({
        widget_margins: [10, 10],
        widget_base_dimensions: [400, 150]
    });

    function applyDashboardsToAllSelectors() {
        if (Object.keys(globalDashboards).length > 0) {
            $("a.add-to-dashboard[data-widget-type]").each(function() {
                var dashboardList = $("ul.dashboard-selector", $(this).parent());
                $("li", dashboardList).remove();

                for (var key in globalDashboards) {
                    var dashboard = globalDashboards[key];
                    var link = "<li><a href='#' data-dashboard-id='" + key + "'>" + htmlEscape(dashboard.title) + "</a></li>"
                    dashboardList.append(link);
                }
            });
        }
    }

    $('ul.dashboard-selector li a[data-dashboard-id]').live("click", function() {
        delegateAddToDashboard($(this).closest("ul.dashboard-selector").attr("data-widget-type"), $(this).attr("data-dashboard-id"));
    })

    function delegateAddToDashboard(widgetType, dashboardId) {
        switch(widgetType) {
            case "search-result-count":
                addSearchResultCountWidget(dashboardId);
                break;
        }
    }

    function addSearchResultCountWidget(dashboardId) {
        var params = originalUniversalSearchSettings();
        params.widgetType = "SEARCH_RESULT_COUNT";

        addWidget(dashboardId, params);
    }

    function addWidget(dashboardId, params) {
        $.ajax({
            url: '/a/dashboards/' + dashboardId + '/widgets',
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

});