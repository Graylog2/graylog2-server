function configureDialog_stats_count(callback) {
    configureDialog_search_result_count(callback);
}

function addWidget_stats_count(dashboardId, config, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "STATS_COUNT";
    params.field =  eventElem.data("field");
    params.statsFunction =  eventElem.data("statistical-function");

    copyInto(config, params);
    addWidget(dashboardId, undefined, params);
}

function updateWidget_stats_count(widget, data) {
    updateWidget_search_result_count(widget, data);
}