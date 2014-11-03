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

function updateWidget_field_chart(widget, data) {

    var graphElem = $('.dashboard-chart', widget);

    /*
     * Convert to Rickshaw-suitable data line array.
     *
     * [{ x: 1893456000, y: 92228531 }, { x: 1577923200, y: 106021568 }]
     */

    var series = [];
    for (var key in data.result) {
        var point = {
            x: parseInt(key),
            y: data.result[key][graphElem.attr("data-config-valuetype")]
        }

        series.push(point);
    }

    if(series.length == 0) {
        graphElem.html("<div class=\"not-available\">N/A</div>");
        return;
    }

    var resolution = graphElem.data("config-interval");
    var fixedTimeAxis = widget.data("fixed-time-axis");

    if (data.time_range != null) {
        var from;
        if (fixedTimeAxis) {
            from = data.time_range.from;
        }
        rickshawHelper.correctDataBoundaries(series, from, data.time_range.to, resolution);
    }

    // we need to replace the entire element that rickshaw touches, otherwise
    // it will leak event listeners and tons of DOM elements
    graphElem.html('<div class="graph_chart">');

    var renderer = graphElem.attr("data-config-renderer");

    var graph = new Rickshaw.Graph( {
        element: $('.graph_chart', graphElem)[0],
        width: 800,
        height: 70,
        interpolation: graphElem.attr("data-config-interpolation"),
        renderer: rickshawHelper.getRenderer(renderer),
        resolution: resolution,
        series: [ {
            name: "value",
            data: series,
            color: '#26ADE4'
        } ]
    });

    new Rickshaw.Graph.Axis.Y( {
        graph: graph,
        tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
        pixelsPerTick: 17
    });

    new Rickshaw.Graph.Axis.Time({
        graph: graph,
        ticksTreatment: "glow",
        timeFixture: new Rickshaw.Fixtures.Graylog2Time(gl2UserTimeZoneOffset) // Cares about correct TZ handling.
    });

    new Rickshaw.Graph.HoverDetail({
        graph: graph,
        formatter: function(series, x, y) {
            field = graphElem.attr("data-config-field");
            var dateMoment = momentHelper.toUserTimeZone(new Date(x * 1000 ));
            var date = '<span class="date">' + dateMoment.format('ddd MMM DD YYYY HH:mm:ss ZZ') + '</span>';
            var swatch = '<span class="detail_swatch"></span>';
            var content = '[' + graphElem.attr("data-config-valuetype") + '] ' + field + ': ' + numeral(y).format('0.[000]') + '<br>' + date;
            return content;
        }
    });

    if (renderer == "scatterplot") {
        graph.renderer.dotSize = 2;
    }

    if (renderer== "area") {
        graph.renderer.stroke = true;
    }

    graph.render();
}