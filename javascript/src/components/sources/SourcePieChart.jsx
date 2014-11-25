'use strict';

var $ = require('jquery');

var dc = require('dc');

var React = require('react');

var SourcePieChart = React.createClass({
    renderPieChart(dimension, group, onDataFiltered) {
        var pieChartDomNode = $("#dc-sources-pie-chart")[0];
        var pieChartWidth = $(pieChartDomNode).width();
        this.pieChart = dc.pieChart(pieChartDomNode);
        this.pieChart
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
    configurePieChartWidth(pieChartWidth) {
        this.pieChart.width(pieChartWidth)
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