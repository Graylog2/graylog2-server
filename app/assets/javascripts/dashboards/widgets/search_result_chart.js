function addWidget_search_result_chart(dashboardId, description, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "SEARCH_RESULT_CHART";
    params.interval = eventElem.attr("data-interval");

    if (!!eventElem.attr("data-stream-id")) {
        params.streamId = eventElem.attr("data-stream-id");
    }

    addWidget(dashboardId, description, params);
}