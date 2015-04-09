function addWidget_quickvalues(dashboardId, description, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "QUICKVALUES";

    params.field = eventElem.closest(".quickvalues").attr("data-field");

    if (!!eventElem.attr("data-stream-id")) {
        params.streamId = eventElem.attr("data-stream-id");
    }

    addWidget(dashboardId, description, params);
}