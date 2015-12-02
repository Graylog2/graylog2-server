/* jshint -W107 */
'use strict';

var $ = require('jquery');

var dc = require('dc');
var numeral = require('numeral');

var React = require('react');

var SourceTitle = require('./SourceTitle');
var D3Utils = require('../../util/D3Utils');

var SourcePieChart = React.createClass({
    renderPieChart(dimension, group, onDataFiltered) {
        var pieChartDomNode = $("#dc-sources-pie-chart")[0];
        var pieChartWidth = $(pieChartDomNode).width();
        this._pieChart = dc.pieChart(pieChartDomNode);
        this._pieChart
            .renderLabel(false)
            .dimension(dimension)
            .group(group)
            .colors(D3Utils.glColourPalette())
            .slicesCap(this.props.numberOfTopValues)
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
                <SourceTitle className="reset" resetFilters={this.props.resetFilters}>Messages per source</SourceTitle>
            </div>
        );
    }
});

module.exports = SourcePieChart;