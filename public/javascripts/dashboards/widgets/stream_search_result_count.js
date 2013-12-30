function addWidget_stream_search_result_count(dashboardId, description, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "STREAM_SEARCH_RESULT_COUNT";
    params.streamId =  eventElem.attr("data-stream-id");

    addWidget(dashboardId, description, params);
}

function updateWidget_stream_search_result_count(widget, data) {
    $(".value", widget).text(numeral(data.result).format());
}