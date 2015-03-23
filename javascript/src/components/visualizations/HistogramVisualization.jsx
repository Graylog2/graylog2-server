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
        this.firstRender = true;
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
        var histogramDomNode = this.getDOMNode();

        this.histogram = dc.barChart(histogramDomNode);
        this.histogram
            .width(810)
            .height(120)
            .margins({left: 35, right: 15, top: 10, bottom: 30})
            .dimension(this.dimension)
            .group(this.group)
            .x(d3.time.scale())
            .elasticX(true)
            .elasticY(true)
            .centerBar(true)
            .renderHorizontalGridLines(true)
            .brushOn(false)
            .xAxisLabel(this._formatInterval())
            .yAxisLabel("Messages")
            .renderTitle(false)
            .colors(D3Utils.glColourPalette())
            .on('renderlet', (_) => {
                var formatTitle = (d) => {
                    return numeral(d.y).format("0,0") + " messages<br>" + d.x.format(momentHelper.DATE_FORMAT_TZ);
                };

                d3.select(histogramDomNode).selectAll('.chart-body rect.bar')
                    .attr('rel', 'tooltip')
                    .attr('data-original-title', formatTitle);
            });

        $(histogramDomNode).tooltip({
            'selector': '[rel="tooltip"]',
            'trigger': 'hover',
            'container': 'body',
            'placement': 'auto',
            'delay': { show: 300, hide: 100 },
            'html': true
        });

        this.histogram.xAxis()
            .ticks(graphHelper.customTickInterval())
            .tickFormat(graphHelper.customDateTimeFormat());
        this.histogram.yAxis()
            .ticks(3)
            .tickFormat((value) => {
                return value % 1 === 0 ? d3.format("s")(value) : null;
            });
        this.histogram.render();
    },
    _formatInterval() {
        return this.props.interval.charAt(0).toUpperCase() + this.props.interval.slice(1) + "s";
    },
    processData(data) {
        var formattedData = [];
        for(var key in data) {
            if (data.hasOwnProperty(key)) {
                formattedData.push({x: Number(key), y: data[key]});
            }
        }
        this.setState({processedData: formattedData}, this.drawData);
    },
    drawData() {
        this.histogram.xUnits(() => this.state.processedData.length - 1);
        this.histogramData.remove();
        this.histogramData.add(this.state.processedData);
        this.histogram.redraw();

        // Fix to make Firefox render tooltips in the right place
        // TODO: Find the cause of this
        if (this.firstRender) {
            this.histogram.render();
            this.firstRender = false;
        }
    },
    render() {
        return (
            <div id={"visualization-" + this.props.id} className="histogram"/>
        );
    }
});

module.exports = HistogramVisualization;