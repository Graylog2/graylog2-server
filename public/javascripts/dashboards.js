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

    function applyDashboardsToAllSelectors() {
        $("a.add-to-dashboard[data-widget-type]").each(function() {
            var dashboardList = $("ul.dashboard-selector", $(this).parent());
            $("li", dashboardList).remove();

            for (var key in globalDashboards) {
                var dashboard = globalDashboards[key];
                var link = "<li><a href='#'>" + htmlEscape(dashboard.title) + "</a></li>"
                dashboardList.append(link);
            }
        });
    }

});