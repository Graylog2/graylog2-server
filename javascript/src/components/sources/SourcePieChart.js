'use strict';

var $ = require('jquery');

var dc = require('dc');

var SourcePieChart = {
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
    }
};

module.exports = SourcePieChart;