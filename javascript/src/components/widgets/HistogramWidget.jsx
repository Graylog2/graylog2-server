/* global graphHelper */

'use strict';

var React = require('react');
var crossfilter = require('crossfilter');
var dc = require('dc');
var d3 = require('d3');
var $ = require('jquery');

var BaseWidget = require('./BaseWidget');

var WidgetsStore = require('../../stores/widgets/WidgetsStore');

var HistogramWidget = React.createClass({
    getInitialState() {
        this.histogramData = crossfilter();
        this.dimension = this.histogramData.dimension((d) => new Date(d.x * 1000));
        this.group = this.dimension.group().reduceSum((d) => d.y);

        return {
            result: undefined,
            calculatedAt: undefined,
            from: undefined,
            to: undefined
        };
    },
    componentDidMount() {
        this.renderHistogram();
    },
    renderHistogram() {
        var histogramDomNode = $("#widget-" + this.props.widgetId)[0];

        this.histogram = dc.barChart(histogramDomNode);
        this.histogram
            .width(800)
            .height(100)
            .margins({left: 35, right: 20, top: 20, bottom: 20})
            .dimension(this.dimension)
            .group(this.group)
            .x(d3.time.scale())
            .renderHorizontalGridLines(true)
            .elasticY(true)
            .transitionDuration(30);

        this.histogram.xAxis()
            .ticks(graphHelper.customTickInterval())
            .tickFormat(graphHelper.customDateTimeFormat());
        this.histogram.yAxis()
            .ticks(4)
            .tickFormat(d3.format("s"));
    },
    loadValue() {
        var dataPromise = WidgetsStore.loadValue(this.props.dashboardId, this.props.widgetId);
        dataPromise.then((value) => {
            var formattedResults = [];
            for(var key in value.result) {
                if (value.result.hasOwnProperty(key)) {
                    formattedResults.push({x: Number(key), y: value.result[key]});
                }
            }
            value.result = formattedResults;
        });
        dataPromise.done((value) => {
            this.setState({
                result: value.result,
                calculatedAt: value.calculated_at,
                from: value.time_range.from,
                to: value.time_range.to
            }, this._resetHistogramData);
        });
    },
    _resetHistogramData() {
        this.histogram.x(d3.time.scale().domain([new Date(this.state.from), new Date(this.state.to)]))
                      .xUnits(d3.time.days);
        this.histogramData.remove();
        this.histogramData.add(this.state.result);
        dc.renderAll();
    },
    render() {
        var widget = (
            <BaseWidget dashboardId={this.props.dashboardId}
                        widgetId={this.props.widgetId}
                        loadValueCallback={this.loadValue}
                        calculatedAt={this.state.calculatedAt}>
                <div id={"widget-" + this.props.widgetId} className="histogram"/>
            </BaseWidget>
        );
        return widget;
    }
});

module.exports = HistogramWidget;