import React from 'react';
import $ from 'jquery';
import {} from 'jquery-ui/ui/effects/effect-bounce';
import dc from 'dc';
import d3 from 'd3';

import UniversalSearch from 'logic/search/UniversalSearch';
import SourceTitle from './SourceTitle';
import D3Utils from 'util/D3Utils';

import graphHelper from 'legacy/graphHelper';

const SourceLineChart = React.createClass({
  propTypes: {
    histogramDataAvailable: React.PropTypes.bool.isRequired,
    reloadingHistogram: React.PropTypes.bool.isRequired,
    resetFilters: React.PropTypes.func.isRequired,
    resolution: React.PropTypes.string.isRequired,
  },

  getInitialState() {
    return {
      lineChartWidth: '100%',
    };
  },

  getFilters() {
    return this._lineChart ? this._lineChart.filters() : [];
  },

  setFilter(filter) {
    this._lineChart.filter(filter);
  },

  clearFilters() {
    this._lineChart.filterAll();
  },

  _configureWidth(lineChartWidth) {
    this._lineChart.width(lineChartWidth);
    this.setState({ lineChartWidth: `${String(lineChartWidth)}px` });
  },

  updateWidth() {
    const $lineChartDomNode = $('#dc-sources-line-chart');
    const lineChartWidth = $lineChartDomNode.width();
    this._configureWidth(lineChartWidth);
  },

  renderLineChart(dimension, group, onDataFiltered) {
    const lineChartDomNode = $('#dc-sources-line-chart')[0];
    const width = $(lineChartDomNode).width();
    $(document).on('mouseup', '#dc-sources-line-chart svg', (event) => {
      $('.timerange-selector-container').effect('bounce', {
        complete: () => {
          // Submit search directly if alt key is pressed.
          if (event.altKey) {
            UniversalSearch.submit();
          }
        },
      });
    });
    this._lineChart = dc.lineChart(lineChartDomNode);
    this._lineChart
      .height(200)
      .margins({ left: 35, right: 20, top: 20, bottom: 20 })
      .dimension(dimension)
      .group(group)
      .x(d3.time.scale())
      .renderHorizontalGridLines(true)
      // FIXME: causes those nasty exceptions when rendering data (one per x axis tick)
      .elasticX(true)
      .elasticY(true)
      .transitionDuration(30)
      .colors(D3Utils.glColourPalette())
      .on('filtered', (chart) => {
        dc.events.trigger(() => {
          const filter = chart.filter();
          onDataFiltered(filter);
        });
      });
    this._configureWidth(width);
    this._lineChart.xAxis()
      .ticks(graphHelper.customTickInterval())
      .tickFormat(graphHelper.customDateTimeFormat());
    this._lineChart.yAxis()
      .ticks(6)
      .tickFormat(d3.format('s'));
  },

  render() {
    const loadingSpinnerStyle = {
      display: this.props.reloadingHistogram ? 'block' : 'none',
      width: this.state.lineChartWidth,
    };
    const loadingSpinner = (
      <div className="sources overlay" style={loadingSpinnerStyle}>
        <i className="fa fa-spin fa-refresh spinner" />
      </div>
    );

    const noDataOverlayStyle = {
      display: this.props.histogramDataAvailable ? 'none' : 'block',
      width: this.state.lineChartWidth,
    };
    const noDataOverlay = (
      <div className="sources overlay" style={noDataOverlayStyle}>Not enough data</div>
    );
    return (
      <div id="dc-sources-line-chart" className="col-md-12">
        <SourceTitle className="reset" resetFilters={this.props.resetFilters}>
          Messages per {this.props.resolution}
        </SourceTitle>
        {loadingSpinner}
        {noDataOverlay}
      </div>
    );
  },
});

export default SourceLineChart;
