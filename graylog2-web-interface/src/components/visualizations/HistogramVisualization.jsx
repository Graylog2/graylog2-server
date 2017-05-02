import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import numeral from 'numeral';
import crossfilter from 'crossfilter';
import dc from 'dc';
import d3 from 'd3';
import deepEqual from 'deep-equal';

import DateTime from 'logic/datetimes/DateTime';
import HistogramFormatter from 'logic/graphs/HistogramFormatter';

import D3Utils from 'util/D3Utils';

import graphHelper from 'legacy/graphHelper';

import $ from 'jquery';
global.jQuery = $;
require('bootstrap/js/tooltip');

const HistogramVisualization = React.createClass({
  propTypes: {
    id: PropTypes.string.isRequired,
    data: PropTypes.object.isRequired,
    config: PropTypes.object.isRequired,
    computationTimeRange: PropTypes.object,
    height: PropTypes.number,
    width: PropTypes.number,
  },
  getInitialState() {
    this.triggerRender = true;
    this.histogramData = crossfilter();
    this.dimension = this.histogramData.dimension(d => d.x);
    this.group = this.dimension.group().reduceSum(d => d.y);

    return {
      dataPoints: [],
    };
  },
  componentDidMount() {
    this.renderHistogram();
    this._updateData(this.props.data);
  },
  componentWillReceiveProps(nextProps) {
    if (deepEqual(this.props, nextProps)) {
      return;
    }

    if (nextProps.height !== this.props.height || nextProps.width !== this.props.width) {
      this._resizeVisualization(nextProps.width, nextProps.height);
    }
    this._updateData(nextProps.data);
  },
  _updateData(data) {
    this.setState({ dataPoints: data }, this.drawData);
  },
  _resizeVisualization(width, height) {
    this.histogram
      .width(width)
      .height(height);
    this.triggerRender = true;
  },
  drawData() {
    const isSearchAll = (this.props.config.timerange.type === 'relative' && this.props.config.timerange.range === 0);
    const dataPoints = HistogramFormatter.format(this.state.dataPoints, this.props.computationTimeRange,
      this.props.config.interval, this.props.width, isSearchAll, null);

    this.histogram.xUnits(() => dataPoints.length - 1);
    this.histogramData.remove();
    this.histogramData.add(dataPoints);

    // Fix to make Firefox render tooltips in the right place
    // TODO: Find the cause of this
    if (this.triggerRender) {
      this.histogram.render();
      this.triggerRender = false;
    } else {
      this.histogram.redraw();
    }
  },
  renderHistogram() {
    const histogramDomNode = ReactDOM.findDOMNode(this);

    this.histogram = dc.barChart(histogramDomNode);
    this.histogram
      .width(this.props.width)
      .height(this.props.height)
      .margins({ left: 50, right: 15, top: 10, bottom: 30 })
      .dimension(this.dimension)
      .group(this.group)
      .x(d3.time.scale())
      .elasticX(true)
      .elasticY(true)
      .centerBar(true)
      .renderHorizontalGridLines(true)
      .brushOn(false)
      .xAxisLabel('Time')
      .yAxisLabel('Messages')
      .renderTitle(false)
      .colors(D3Utils.glColourPalette())
      .on('renderlet', () => {
        const formatTitle = (d) => {
          const valueText = `${numeral(d.y).format('0,0')} messages`;
          const keyText = `<span class="date">${new DateTime(d.x).toString(DateTime.Formats.COMPLETE)}</span>`;

          return `<div class="datapoint-info">${valueText}<br>${keyText}</div>`;
        };

        d3.select(histogramDomNode).selectAll('.chart-body rect.bar')
          .attr('rel', 'tooltip')
          .attr('data-original-title', formatTitle);
      });

    $(histogramDomNode).tooltip({
      selector: '[rel="tooltip"]',
      container: 'body',
      placement: 'auto',
      delay: { show: 300, hide: 100 },
      html: true,
    });

    this.histogram.xAxis()
      .ticks(graphHelper.customTickInterval())
      .tickFormat(graphHelper.customDateTimeFormat());
    this.histogram.yAxis()
      .ticks(3)
      .tickFormat((value) => {
        return value % 1 === 0 ? d3.format('s')(value) : null;
      });
    this.histogram.render();
  },
  render() {
    return (
      <div id={`visualization-${this.props.id}`} className="histogram" />
    );
  },
});

export default HistogramVisualization;
