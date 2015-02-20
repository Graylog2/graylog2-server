/* global graphHelper, momentHelper */

'use strict';

var React = require('react');
var numeral = require('numeral');
var crossfilter = require('crossfilter');
var dc = require('dc');
var d3 = require('d3');
var $ = require('jquery');

var D3Utils = require('../../util/D3Utils');

var HistogramVisualization = React.createClass({
    getInitialState() {
        this.histogramData = crossfilter();
        this.dimension = this.histogramData.dimension((d) => momentHelper.toUserTimeZone(d.x * 1000));
        this.group = this.dimension.group().reduceSum((d) => d.y);

        return {
            processedData: []
        };
    },
    componentDidMount() {
        this.renderHistogram();
    },
    componentWillReceiveProps(nextProps) {
        this.processData(nextProps.data);
    },
    renderHistogram() {
        var histogramDomNode = $("#visualization-" + this.props.id)[0];

        this.histogram = dc.barChart(histogramDomNode);
        this.histogram
            .width(800)
            .height(100)
            .margins({left: 45, right: 20, top: 10, bottom: 30})
            .dimension(this.dimension)
            .group(this.group)
            .x(d3.time.scale())
            .elasticX(true)
            .elasticY(true)
            .renderHorizontalGridLines(true)
            .brushOn(false)
            .xAxisLabel(this._formatInterval())
            .yAxisLabel("Messages")
            .renderTitle(false)
            .colors(D3Utils.glColourPalette())
            // TODO: Extract this to a colour palette
            .on('renderlet', (_) => {
                d3.selectAll('.bar').attr('title', (d) => {
                    return numeral(d.y).format("0,0") + " messages<br>" + d.x.format(momentHelper.DATE_FORMAT_TZ);
                });

                $('svg .bar').tooltip({
                    'trigger': 'hover',
                    'container': 'body',
                    'placement': 'top',
                    'delay': { show: 300, hide: 100 },
                    'html': true
                });

            });

        this.histogram.xAxis()
            .ticks(graphHelper.customTickInterval())
            .tickFormat(graphHelper.customDateTimeFormat());
        this.histogram.yAxis()
            .ticks(3)
            .tickFormat(d3.format("s"));
        dc.renderAll();
    },
    _formatInterval() {
        return this.props.interval.charAt(0).toUpperCase() + this.props.interval.slice(1) + "s";
    },
    processData(data) {
        var formattedData = [];
        // TODO: add also points where the number of messages is 0
        for(var key in data) {
            if (data.hasOwnProperty(key)) {
                formattedData.push({x: Number(key), y: data[key]});
            }
        }
        this.setState({processedData: formattedData}, this.drawData);
    },
    drawData() {
        this.histogram.xUnits(() => this.state.processedData.length);
        this.histogramData.remove();
        this.histogramData.add(this.state.processedData);
        this.histogram.redraw();
    },
    render() {
        return (
            <div id={"visualization-" + this.props.id} className="histogram"/>
        );
    }
});

module.exports = HistogramVisualization;