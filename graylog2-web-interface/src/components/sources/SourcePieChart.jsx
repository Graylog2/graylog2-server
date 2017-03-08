import $ from 'jquery';
import dc from 'dc';
import numeral from 'numeral';
import React, { PropTypes } from 'react';

import SourceTitle from './SourceTitle';
import D3Utils from 'util/D3Utils';

const SourcePieChart = React.createClass({
  propTypes: {
    resetFilters: PropTypes.func.isRequired,
    numberOfTopValues: PropTypes.number.isRequired,
  },

  getFilters() {
    return this._pieChart ? this._pieChart.filters() : [];
  },

  setFilter(filter) {
    this._pieChart.filter(filter);
  },

  clearFilters() {
    this._pieChart.filterAll();
  },

  redraw() {
    this._pieChart.redraw();
  },

  _configureWidth(pieChartWidth) {
    this._pieChart.width(pieChartWidth)
      .height(pieChartWidth)
      .radius(pieChartWidth / 2 - 10)
      .innerRadius(pieChartWidth / 5);
  },

  updateWidth() {
    const $pieChartDomNode = $('#dc-sources-pie-chart').parent();
    const pieChartWidth = $pieChartDomNode.width();
    this._configureWidth(pieChartWidth);
  },

  renderPieChart(dimension, group, onDataFiltered) {
    const pieChartDomNode = $('#dc-sources-pie-chart')[0];
    const pieChartWidth = $(pieChartDomNode).width();
    this._pieChart = dc.pieChart(pieChartDomNode);
    this._pieChart
      .renderLabel(false)
      .dimension(dimension)
      .group(group)
      .colors(D3Utils.glColourPalette())
      .slicesCap(this.props.numberOfTopValues)
      .title((d) => {
        return `${d.key}: ${numeral(d.value).format('0,0')}`;
      })
      .on('renderlet', (chart) => {
        chart.selectAll('.pie-slice').on('click', () => {
          onDataFiltered();
        });
      });
    this._configureWidth(pieChartWidth);
  },

  render() {
    return (
      <div id="dc-sources-pie-chart" ref="sourcePieChart">
        <SourceTitle className="reset" resetFilters={this.props.resetFilters}>Messages per source</SourceTitle>
      </div>
    );
  },
});

export default SourcePieChart;
