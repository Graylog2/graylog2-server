/* jshint -W107 */
'use strict';

var $ = require('jquery');

var dc = require('dc');

var React = require('react');

var SourcePieChart = React.createClass({
    renderPieChart(dimension, group, onDataFiltered) {
        var pieChartDomNode = $("#dc-sources-pie-chart")[0];
        var pieChartWidth = $(pieChartDomNode).width();
        this._pieChart = dc._pieChart(pieChartDomNode);
        this._pieChart
            .renderLabel(false)
            .dimension(dimension)
            .group(group)
            .renderlet((chart) => {
                chart.selectAll(".pie-slice").on("click", () => {
                    onDataFiltered();
                });
            });
        this.configurePieChartWidth(pieChartWidth);
    },
    redraw() {
        this._pieChart.redraw();
    },
    clearFilters() {
        this._pieChart.filterAll();
    },
    getFilters() {
        return this._pieChart ? this._pieChart.filters() : [];
    },
    setFilter(filter) {
        this._pieChart.filter(filter);
    },
    configurePieChartWidth(pieChartWidth) {
        this._pieChart.width(pieChartWidth)
            .height(pieChartWidth)
            .radius(pieChartWidth / 2 - 10)
            .innerRadius(pieChartWidth / 5);
    },
    render() {
        return (
            <div id="dc-sources-pie-chart" ref="sourcePieChart">
                <h3><i className="icon icon-bar-chart"></i> Messages per source&nbsp;
                    <small><a href="javascript:undefined" className="reset" onClick={this.props.resetFilters} title="Reset filter" style={{"display": "none"}}><i className="icon icon-retweet"></i></a></small>
                </h3>
            </div>
        );
    }
});

module.exports = SourcePieChart;