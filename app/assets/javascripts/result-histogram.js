resultHistogram = {
    _histogram: [],
    _histogramContainer: $("#result-graph"),
    _yAxis: $("#y_axis"),
    _graphTimeline: $("#result-graph-timeline"),
    _resultHistogramGraph: undefined,

    _correctDataBoundaries: function(data) {
        var selectedResolution = $(".date-histogram-res-selector.selected-resolution").data("resolution");

        rickshawHelper.processHistogramData(data, this._histogramContainer.data("from"), this._histogramContainer.data("to"), selectedResolution);
        return data;
    },

    _getHistogramContainerWidth: function() {
        return this._histogramContainer.width();
    },

    setData: function(data) {
        this._histogram = this._correctDataBoundaries(data);
    },

    drawResultGraph: function() {
        if (this._histogramContainer.length === 0) {
            return;
        }

        if (typeof this._resultHistogramGraph !== 'undefined') {
            return;
        }

        this._histogramContainer.html("");
        this._yAxis.html("");
        this._graphTimeline.html("");

        var selectedResolution = $(".date-histogram-res-selector.selected-resolution").data("resolution");

        var resultGraph = new Rickshaw.Graph( {
            element: this._histogramContainer[0],
            width: this._getHistogramContainerWidth(),
            height: 175,
            renderer: rickshawHelper.getRenderer("bar"),
            resolution: selectedResolution,
            series: [ {
                name: "Messages",
                data: this._histogram,
                color: '#26ADE4'
            } ]
        });

        new Rickshaw.Graph.Axis.Y( {
            graph: resultGraph,
            tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
            orientation: 'left',
            element: this._yAxis[0],
            pixelsPerTick: 30
        });

        // Only show a x-axis (time) when there is more than one bucket.
        if(resultGraph.series != undefined && resultGraph.series[0] != undefined && resultGraph.series[0].data.length > 1) {
            new Rickshaw.Graph.Axis.Time({
                graph: resultGraph,
                ticksTreatment: "glow",
                timeFixture: new Rickshaw.Fixtures.Graylog2Time() // Cares about correct TZ handling.
            });
        }

        new Rickshaw.Graph.HoverDetail({
            graph: resultGraph,
            formatter: function(series, x, y) {
                var dateMoment = momentHelper.toUserTimeZone(new Date(x * 1000 ));
                var date = '<span class="date">' + dateMoment.format('ddd MMM DD YYYY HH:mm:ss ZZ') + '</span>';
                var swatch = '<span class="detail_swatch"></span>';
                var content = numeral(parseInt(y)).format("0,0") + ' messages<br>' + date;
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
            element: this._graphTimeline[0]
        });

        fillAlertAnnotator(resultGraph, annotator);

        resultGraph.render();

        this._resultHistogramGraph = resultGraph;
    },

    redrawResultGraph: function() {
        if(this._histogram.length > 0) {
            if (typeof this._resultHistogramGraph !== 'undefined') {
                this._resultHistogramGraph.configure({width: this._getHistogramContainerWidth()});
                this._resultHistogramGraph.render();
            }
        }
    }
};

$(document).ready(function() {
    if (typeof resultHistogramData != "undefined") {
        resultHistogram.setData(resultHistogramData);
        resultHistogram.drawResultGraph();
    }
});