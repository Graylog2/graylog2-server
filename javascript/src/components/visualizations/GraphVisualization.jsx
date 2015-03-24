/* global graphHelper, momentHelper */

'use strict';

var React = require('react');
var numeral = require('numeral');
var crossfilter = require('crossfilter');
var dc = require('dc');
var d3 = require('d3');
var $ = require('jquery');

var NumberUtils = require("../../util/NumberUtils");
var D3Utils = require('../../util/D3Utils');

var GraphFactory = {
    create(renderer, domNode) {
        var graph;
        switch(renderer) {
            case 'line':
                graph = dc.lineChart(domNode);
                this.tooltipRenderlet(graph, '.chart-body circle.dot');
                break;
            case 'area':
                graph = dc.lineChart(domNode);
                graph.renderArea(true);
                this.tooltipRenderlet(graph, '.chart-body circle.dot');
                break;
            case 'bar':
                graph = dc.barChart(domNode);
                graph.centerBar(true);
                this.tooltipRenderlet(graph, '.chart-body rect.bar');
                break;
            case 'scatterplot':
                graph = dc.scatterPlot(domNode);
                graph
                    .symbolSize(5)
                    .highlightedSize(10)
                    // We need accessors to workaround this: https://github.com/dc-js/dc.js/issues/870
                    .keyAccessor((d) => d.key)
                    .valueAccessor((d) => d.value);
                this.tooltipRenderlet(graph, '.chart-body path.symbol');
                break;
            default:
                throw "Unsupported renderer '" + renderer + "'";
        }

        // Bar charts with clip padding overflow the x axis
        if(renderer !== 'bar') {
            graph.clipPadding(5);
        }

        return graph;
    },
    // Add a data element to the given D3 selection to show a bootstrap tooltip
    tooltipRenderlet(graph, selector) {
        graph.on('renderlet', (chart) => {
            var formatTitle = (d) => {
                var formattedKey = d.x === undefined ? d.x : d.x.format(momentHelper.DATE_FORMAT_TZ);

                var formattedValue;
                try {
                    formattedValue = numeral(d.y).format("0,0.[00]");
                } catch (e) {
                    formattedValue = d3.format(".2r")(d.y);
                }

                return formattedValue + " messages<br>" + formattedKey;
            };

            d3.select(chart.root()[0][0]).selectAll(selector)
                .attr('rel', 'tooltip')
                .attr('data-original-title', formatTitle);
        });
    }
};

var GraphVisualization = React.createClass({
    getInitialState() {
        this.firstRender = true;
        this.graphData = crossfilter();
        this.dimension = this.graphData.dimension((d) => momentHelper.toUserTimeZone(d.x * 1000));
        this.group = this.dimension.group().reduceSum((d) => d.y);

        return {
            valueType: undefined,
            interpolation: undefined,
            processedData: []
        };
    },
    componentDidMount() {
        this.renderGraph();
    },
    componentWillReceiveProps(nextProps) {
        this.processData(nextProps.data);
    },
    renderGraph() {
        var graphDomNode = this.getDOMNode();

        this.graph = GraphFactory.create(this.props.config.renderer, graphDomNode);
        this.graph
            .width(810)
            .height(120)
            .margins({left: 50, right: 15, top: 10, bottom: 35})
            .dimension(this.dimension)
            .group(this.group)
            .x(d3.time.scale())
            .elasticX(true)
            .elasticY(true)
            .renderHorizontalGridLines(true)
            .brushOn(false)
            .xAxisLabel(this._formatInterval())
            .yAxisLabel(this.props.config.field)
            .renderTitle(false)
            .colors(D3Utils.glColourPalette());

        $(graphDomNode).tooltip({
            'selector': '[rel="tooltip"]',
            'trigger': 'hover',
            'container': 'body',
            'placement': 'auto',
            'delay': { show: 300, hide: 100 },
            'html': true
        });

        this.graph.xAxis()
            .ticks(graphHelper.customTickInterval())
            .tickFormat(graphHelper.customDateTimeFormat());
        this.graph.yAxis()
            .ticks(3)
            .tickFormat((value) => {
                return Math.abs(value) > 1e+30 ? value.toPrecision(1) : d3.format(".2s")(value);
            });
        this.graph.render();
    },
    _formatInterval() {
        return this.props.config.interval.charAt(0).toUpperCase() + this.props.config.interval.slice(1) + "s";
    },
    processData(data) {
        var formattedData = [];
        for(var key in data) {
            if (data.hasOwnProperty(key)) {
                var normalizedValue = NumberUtils.normalizeNumber(data[key]);
                formattedData.push({
                    x: Number(key),
                    y: isNaN(normalizedValue) ? 0 : normalizedValue
                });
            }
        }
        this.setState({processedData: formattedData}, this.drawData);
    },
    drawData() {
        this.graph.xUnits(() => Math.max(this.state.processedData.length - 1, 1));
        this.graphData.remove();
        this.graphData.add(this.state.processedData);
        this.graph.redraw();

        // Fix to make Firefox render tooltips in the right place
        // TODO: Find the cause of this
        if (this.firstRender) {
            this.graph.render();
            this.firstRender = false;
        }
    },
    render() {
        return (
            <div id={"visualization-" + this.props.id} className="graph"/>
        );
    }
});

module.exports = GraphVisualization;