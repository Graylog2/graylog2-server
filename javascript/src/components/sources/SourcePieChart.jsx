/* jshint -W107 */
'use strict';

var $ = require('jquery');

var dc = require('dc');
var numeral = require('numeral');

var React = require('react');

var SourcePieChart = React.createClass({
    renderPieChart(dimension, group, onDataFiltered) {
        var pieChartDomNode = $("#dc-sources-pie-chart")[0];
        var pieChartWidth = $(pieChartDomNode).width();
        this._pieChart = dc.pieChart(pieChartDomNode);
        this._pieChart
            .renderLabel(false)
            .dimension(dimension)
            .group(group)
            .title((d) => { return d.key + ": " + numeral(d.value).format("0,0"); })
            .on('renderlet', (chart) => {
                chart.selectAll(".pie-slice").on("click", () => {
                    onDataFiltered();
                });
            });
        this._configureWidth(pieChartWidth);
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
    _configureWidth(pieChartWidth) {
        this._pieChart.width(pieChartWidth)
            .height(pieChartWidth)
            .radius(pieChartWidth / 2 - 10)
            .innerRadius(pieChartWidth / 5);
    },
    updateWidth() {
        var pieChartDomNode = $("#dc-sources-pie-chart").parent();
        var pieChartWidth = pieChartDomNode.width();
        this._configureWidth(pieChartWidth);
    },
    render() {
        return (
            <div id="dc-sources-pie-chart" ref="sourcePieChart">
                <h3><i className="fa fa-bar-chart"></i> Messages per source&nbsp;
                    <small><a href="javascript:undefined" className="reset" onClick={this.props.resetFilters} title="Reset filter" style={{"display": "none"}}><i className="fa fa-remove"></i> Reset filter</a></small>
                </h3>
            </div>
        );
    }
});

module.exports = SourcePieChart;