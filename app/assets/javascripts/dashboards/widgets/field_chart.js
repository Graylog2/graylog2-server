function addWidget_field_chart(dashboardId, description, eventElem) {
    var chart = eventElem.closest(".field-graph-container");
    var chartOpts = JSON.parse(chart.attr("data-lines"));

    var params = {};
    params.widgetType = "FIELD_CHART";

    params.valuetype = chartOpts.valuetype;
    params.renderer = chartOpts.renderer;
    params.interpolation = chartOpts.interpolation;
    params.interval = chartOpts.interval;
    params.field = chartOpts.field;

    params.query = chartOpts.query;
    params.rangeType = chartOpts.rangetype;

    switch(params.rangeType) {
        case "relative":
            params.relative = chartOpts.range.relative;
            break;
        case "absolute":
            params.from = chartOpts.range.from;
            params.to = chartOpts.range.to;
            break;
        case "keyword":
            params.keyword = chartOpts.range.keyword;
            break;
    }

    if (!!eventElem.attr("data-stream-id")) {
        params.streamId = eventElem.attr("data-stream-id");
    }

    addWidget(dashboardId, description, params);
}