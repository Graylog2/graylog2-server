// We need to override Rickshaw _frequentInterval detection for bar charts due to this issue:
// https://github.com/shutterstock/rickshaw/issues/461
var ResultHistogramRenderer = Rickshaw.Class.create(Rickshaw.Graph.Renderer.Bar, {
    _frequentInterval: function(data) {
        var selectedResolution = $(".date-histogram-res-selector.selected-resolution").data("resolution");
        var resolutionDuration = moment.duration(1, selectedResolution);
        return { count: 1, magnitude: resolutionDuration.asSeconds() };
    }
});

resultHistogram = {
    _histogram: [],

    // Show the whole search time range, even if no data is available
    _correctDataBoundaries: function(data) {
        var resultGraphElement = $("#result-graph");
        var fromMoment = moment.utc(resultGraphElement.data("from"));
        var toMoment = moment.utc(resultGraphElement.data("to"));

        var selectedResolution = $(".date-histogram-res-selector.selected-resolution").data("resolution");
        if (selectedResolution == "week") {
           selectedResolution = "isoWeek"; // Weeks should start on Monday :)
        }
        var fromFormatted = fromMoment.startOf(selectedResolution).unix();
        var toFormatted = toMoment.startOf(selectedResolution).unix();

        if (fromFormatted != data[0].x) {
            data.unshift({"x": fromFormatted, "y": 0});
        }
        if ((toFormatted != data[data.length-1].x) && (toFormatted != fromFormatted)) {
            data.push({"x": toFormatted, "y": 0});
        }

        return data;
    },

    setData: function(data) {
        this._histogram = this._correctDataBoundaries(data);
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
            renderer: ResultHistogramRenderer,
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