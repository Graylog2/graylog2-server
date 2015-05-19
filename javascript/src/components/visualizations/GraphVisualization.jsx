/* global graphHelper, momentHelper */

'use strict';

var React = require('react');
var numeral = require('numeral');
var crossfilter = require('crossfilter');
var dc = require('dc');
var d3 = require('d3');
var $ = require('jquery');

var D3Utils = require('../../util/D3Utils');
var StringUtils = require('../../util/StringUtils');

var GraphFactory = {
    create(config, domNode, tooltipTitleFormatter) {
        var graph;
        switch (config.renderer) {
            case 'line':
                graph = dc.lineChart(domNode);
                D3Utils.tooltipRenderlet(graph, '.chart-body circle.dot', tooltipTitleFormatter);
                break;
            case 'area':
                graph = dc.lineChart(domNode);
                graph.renderArea(true);
                D3Utils.tooltipRenderlet(graph, '.chart-body circle.dot', tooltipTitleFormatter);
                break;
            case 'bar':
                graph = dc.barChart(domNode);
                graph.centerBar(true);
                D3Utils.tooltipRenderlet(graph, '.chart-body rect.bar', tooltipTitleFormatter);
                break;
            case 'scatterplot':
                graph = dc.lineChart(domNode);
                graph.renderDataPoints({radius: 2, fillOpacity: 1, strokeOpacity: 1});
                D3Utils.tooltipRenderlet(graph, '.chart-body circle.dot', tooltipTitleFormatter);
                break;
            default:
                throw "Unsupported renderer '" + config.renderer + "'";
        }

        if (config.renderer === 'line' || config.renderer === 'area') {
            graph.interpolate(config.interpolation);
        }

        // Bar charts with clip padding overflow the x axis
        if (config.renderer !== 'bar') {
            graph.clipPadding(5);
        }

        return graph;
    }
};

var GraphVisualization = React.createClass({
    getInitialState() {
        this.triggerRender = true;
        this.graphData = crossfilter();
        this.dimension = this.graphData.dimension((d) => momentHelper.toUserTimeZone(d.x * 1000));
        this.group = this.dimension.group().reduceSum((d) => d.y);

        return {
            valueType: undefined,
            interpolation: undefined,
            dataPoints: []
        };
    },
    componentDidMount() {
        this.renderGraph();
    },
    componentWillReceiveProps(nextProps) {
        if (nextProps.height !== this.props.height || nextProps.width !== this.props.width) {
            this._resizeVisualization(nextProps.width, nextProps.height);
        }
        this.setState({dataPoints: nextProps.data}, this.drawData);
    },
    renderGraph() {
        var graphDomNode = React.findDOMNode(this);

        this.graph = GraphFactory.create(this.props.config, graphDomNode, this._formatTooltipTitle);
        this.graph
            .width(this.props.width)
            .height(this.props.height)
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
            'container': 'body',
            'placement': 'auto',
            'delay': {show: 300, hide: 100},
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
    _formatTooltipTitle(d) {
        var formattedKey = d.x === undefined ? d.x : d.x.format(momentHelper.HUMAN_TZ);

        var formattedValue;
        try {
            formattedValue = numeral(d.y).format("0,0.[00]");
        } catch (e) {
            formattedValue = d3.format(".2r")(d.y);
        }

        var valueText = this.props.config.valuetype + " " + this.props.config.field + ": " + formattedValue + "<br>";
        var keyText = "<span class=\"date\">" + formattedKey + "</span>";

        return "<div class=\"datapoint-info\">" + valueText + keyText + "</div>";
    },
    _formatInterval() {
        return StringUtils.capitalizeFirstLetter(this.props.config.interval) + "s";
    },
    _resizeVisualization(width, height) {
        this.graph
            .width(width)
            .height(height);
        this.triggerRender = true;
    },
    drawData() {
        this.graph.xUnits(() => Math.max(this.state.dataPoints.length - 1, 1));
        this.graphData.remove();
        this.graphData.add(this.state.dataPoints);

        // Fix to make Firefox render tooltips in the right place
        // TODO: Find the cause of this
        if (this.triggerRender) {
            this.graph.render();
            this.triggerRender = false;
        } else {
            this.graph.redraw();
        }
    },
    render() {
        return (
            <div id={"visualization-" + this.props.id} className={"graph " + this.props.config.renderer}/>
        );
    }
});

module.exports = GraphVisualization;