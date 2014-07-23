resultHistogram = {
    _histogram: [],

    setData: function(data) {
        this._histogram = data;
    },

    drawResultGraph: function() {
        var resultGraphElement = $("#result-graph");
        if (resultGraphElement.length == 0) {
            return;
        }

        resultGraphElement.html("");
        $("#result-graph-timeline" ).html("");

        var graphWidth = resultGraphElement.width();

        var resultGraph = new Rickshaw.Graph( {
            element: resultGraphElement.get(0),
            width: graphWidth,
            height: 175,
            renderer: "bar",
            series: [ {
                name: "Messages",
                data: this._histogram,
                color: '#26ADE4'
            } ]
        });

        new Rickshaw.Graph.Axis.Y( {
            graph: resultGraph,
            tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
            pixelsPerTick: 30
        });

        // Only show a x-axis (time) when there is more than one bucket.
        if(resultGraph.series != undefined && resultGraph.series[0] != undefined && resultGraph.series[0].data.length > 1) {
            new Rickshaw.Graph.Axis.Time({
                graph: resultGraph,
                ticksTreatment: "glow",
                timeFixture: new Rickshaw.Fixtures.Graylog2Time(gl2UserTimeZoneOffset) // Cares about correct TZ handling.
            });
        }

        new Rickshaw.Graph.HoverDetail({
            graph: resultGraph,
            formatter: function(series, x, y) {
                var dateMoment = moment(new Date(x * 1000 )).zone(gl2UserTimeZoneOffset);
                var date = '<span class="date">' + dateMoment.format('ddd MMM DD YYYY HH:mm:ss ZZ') + '</span>';
                var swatch = '<span class="detail_swatch"></span>';
                var content = parseInt(y) + ' messages<br>' + date;
                return content;
            },
            xFormatter: function(x) {
                return new Date(x * 1000).toDateString();
            }
        });

        new Rickshaw.Graph.Graylog2Selector( {
            graph: resultGraph
        });

        var annotator = new Rickshaw.Graph.Annotate({
            graph: resultGraph,
            element: document.getElementById('result-graph-timeline')
        });

        fillAlertAnnotator(resultGraph, annotator);

        resultGraph.render();
    },

    redrawResultGraph: function() {
        if(this._histogram.length > 0) {
            this.drawResultGraph();
        }
    }
};

$(document).ready(function() {
    if (typeof resultHistogramData != "undefined") {
        resultHistogram.setData(resultHistogramData);
        resultHistogram.drawResultGraph();
    }
});