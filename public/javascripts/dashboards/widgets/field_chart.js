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

    graphElem.html("");

    var renderer = graphElem.attr("data-config-renderer");

    var graph = new Rickshaw.Graph( {
        element: graphElem.get()[0],
        width: 800,
        height: 70,
        interpolation: graphElem.attr("data-config-interpolation"),
        renderer: renderer,
        series: [ {
            name: "value",
            data: series,
            color: '#26ADE4'
        } ]
    });

    new Rickshaw.Graph.Axis.Y( {
        graph: graph,
        tickFormat: Rickshaw.Fixtures.Number.formatKMBT
    });

    new Rickshaw.Graph.Axis.Time({
        graph: graph,
        ticksTreatment: "glow"
    });

    new Rickshaw.Graph.HoverDetail({
        graph: graph,
        formatter: function(series, x, y) {
            field = graphElem.attr("data-config-field");
            var date = '<span class="date">' + new Date(x * 1000).toUTCString() + '</span>';
            var swatch = '<span class="detail_swatch"></span>';
            var content = field + ': ' + parseInt(y) + '<br>' + date;
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