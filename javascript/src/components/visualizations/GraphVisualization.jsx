/* global graphHelper, momentHelper */

'use strict';

var React = require('react');
var numeral = require('numeral');
var crossfilter = require('crossfilter');
var dc = require('dc');
var d3 = require('d3');
var $ = require('jquery');

var D3Utils = require('../../util/D3Utils');

var GraphFactory = {
    create(renderer, domNode) {
        var graph;
        switch(renderer) {
            case 'line':
                graph = dc.lineChart(domNode);
                break;
            case 'bar':
                graph = dc.barChart(domNode);
                graph.centerBar(true);
            default:
                console.log("Unsupported renderer '" + renderer +"'");
        }

        return graph;
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
            .margins({left: 35, right: 15, top: 10, bottom: 30})
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
            .colors(D3Utils.glColourPalette())
            .on('renderlet', (_) => {
                var formatTitle = (d) => {
                    return numeral(d.y).format("0,0.[00]") + " messages<br>" + d.x.format(momentHelper.DATE_FORMAT_TZ);
                };

                d3.select(graphDomNode).selectAll('.chart-body circle.dot')
                    .attr('rel', 'tooltip')
                    .attr('data-original-title', formatTitle);
            });

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
            .tickFormat(d3.format("s"));
        this.graph.render();
    },
    _formatInterval() {
        return this.props.config.interval.charAt(0).toUpperCase() + this.props.config.interval.slice(1) + "s";
    },
    processData(data) {
        var formattedData = [];
        for(var key in data) {
            formattedData.push({x: Number(key), y: data[key][this.props.config.valuetype]});
        }
        this.setState({processedData: formattedData}, this.drawData);
    },
    drawData() {
        this.graph.xUnits(() => this.state.processedData.length - 1);
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