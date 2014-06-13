function addWidget_search_result_chart(dashboardId, description, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "SEARCH_RESULT_CHART";
    params.interval = eventElem.attr("data-interval");

    if (!!eventElem.attr("data-stream-id")) {
        params.streamId = eventElem.attr("data-stream-id");
    }

    addWidget(dashboardId, description, params);
}

function updateWidget_search_result_chart(widget, data) {

    var graphElem = $('.dashboard-search-result-chart', widget);

    /*
     * Convert to Rickshaw-suitable data line array.
     *
     * [{ x: 1893456000, y: 92228531 }, { x: 1577923200, y: 106021568 }]
     */

    var series = [];
    for (var key in data.result) {
        var point = {
            x: parseInt(key),
            y: data.result[key]
        }

        series.push(point);
    }

    if(series.length == 0) {
        return;
    }

    // we need to replace the entire element that rickshaw touches, otherwise
    // it will leak event listeners and tons of DOM elements
    graphElem.html('<div class="graph_chart">');

    var graph = new Rickshaw.Graph( {
        element: $('.graph_chart', graphElem)[0],
        width: 800,
        height: 70,
        renderer: "bar",
        series: [ {
            name: "Messages",
            data: series,
            color: '#26ADE4'
        } ]
    });

    new Rickshaw.Graph.Axis.Y( {
        graph: graph,
        tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
        pixelsPerTick: 17
    });

    // Only show a x-axis (time) when there is more than one bucket.
    if(graph.series != undefined && graph.series[0] != undefined && graph.series[0].data.length > 1) {
        new Rickshaw.Graph.Axis.Time({
            graph: graph,
            ticksTreatment: "glow",
            timeFixture: new Rickshaw.Fixtures.Graylog2Time(gl2UserTimeZoneOffset) // Cares about correct TZ handling.
        });
    }

    new Rickshaw.Graph.HoverDetail({
        graph: graph,
        formatter: function(series, x, y) {
            var date = '<span class="date">' + new Date(x * 1000 ).toString() + '</span>';
            var swatch = '<span class="detail_swatch"></span>';
            var content = parseInt(y) + ' messages<br>' + date;
            return content;
        },
        xFormatter: function(x) {
            return new Date(x * 1000).toDateString();
        }
    });

    graph.render();
}