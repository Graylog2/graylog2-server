function addWidget_stream_search_result_count(dashboardId, description, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "STREAM_SEARCH_RESULT_COUNT";
    params.streamId =  eventElem.attr("data-stream-id");

    addWidget(dashboardId, description, params);
}

function updateWidget_stream_search_result_count(widget) {
    var dashboardId = widget.attr("data-dashboard-id");
    var widgetId = widget.attr("data-widget-id");

    $(".reloading", widget).show();

    $.ajax({
        url: '/a/dashboards/' + dashboardId + '/widgets/' + widgetId + '/value',
        type: 'GET',
        success: function(data) {
            $(".value", widget).text(numeral(data.result).format());
            $(".calculated-at", widget).attr("title", data.calculated_at);
            $(".calculated-at", widget).text(moment(data.calculated_at).fromNow());
        },
        error: function(data) {
            widget.attr("data-disabled", "true");
            showErrorInWidget(widget);
        },
        complete: function(data) {
            $(".reloading", widget).hide();
        }
    });
}