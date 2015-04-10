function configureDialog_stats_count(callback, element) {
    var field =  element.data("field");
    var statsFunction =  element.data("statistical-function");
    var description = statsFunction + " on " + field;
    configureDialog_search_result_count(callback, element, description);
}

function addWidget_stats_count(dashboardId, config, eventElem) {
    var params = originalUniversalSearchSettings();
    var streamId = eventElem.data("stream-id");
    if (streamId) {
        params.streamId = streamId;
    }
    params.widgetType = "STATS_COUNT";
    params.field =  eventElem.data("field");
    params.statsFunction =  eventElem.data("statistical-function");

    copyInto(config, params);
    addWidget(dashboardId, undefined, params);
}