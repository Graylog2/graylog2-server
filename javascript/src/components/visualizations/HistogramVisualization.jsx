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
            .height(120)
            .margins({left: 35, right: 0, top: 10, bottom: 30})
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
            .on('renderlet', (_) => {
                $('svg .chart-body rect.bar', histogramDomNode).tooltip({
                    'trigger': 'hover',
                    'container': 'body',
                    'placement': 'top',
                    'delay': { show: 300, hide: 100 },
                    'html': true
                });

                var formatTitle = (d) => {
                    return numeral(d.y).format("0,0") + " messages<br>" + d.x.format(momentHelper.DATE_FORMAT_TZ);
                };

                d3.select(histogramDomNode).selectAll('.chart-body rect.bar')
                    .attr('data-original-title', formatTitle);
            });

        this.histogram.xAxis()
            .ticks(graphHelper.customTickInterval())
            .tickFormat(graphHelper.customDateTimeFormat());
        this.histogram.yAxis()
            .ticks(3)
            .tickFormat((value) => {
                return value % 1 === 0 ? d3.format("s")(value) : null;
            });
        dc.renderAll();
    },
    _formatInterval() {
        return this.props.interval.charAt(0).toUpperCase() + this.props.interval.slice(1) + "s";
    },
    processData(data) {
        var formattedData = [];
        for(var key in data) {
            formattedData.push({x: Number(key), y: data[key]});
        }
        this.setState({processedData: formattedData}, this.drawData);
    },
    drawData() {
        this.histogram.xUnits(() => this.state.processedData.length - 1);
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