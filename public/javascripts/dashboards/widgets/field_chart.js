function addWidget_field_chart(dashboardId, description, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "FIELD_CHART";
    params.field = eventElem.attr("data-field");

    var chart = eventElem.closest(".field-graph-container");

    params.valuetype = chart.attr("data-config-valuetype");
    params.renderer = chart.attr("data-config-renderer");
    params.interpolation = chart.attr("data-config-interpolation");
    params.interval = chart.attr("data-config-interval");

    if (!!eventElem.attr("data-stream-id")) {
        params.streamId =  eventElem.attr("data-stream-id");
    }

    addWidget(dashboardId, description, params);
}

function updateWidget_field_chart(widget, data) {
    console.log(data);
}