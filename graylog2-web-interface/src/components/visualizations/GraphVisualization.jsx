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
import NumberUtils from 'util/NumberUtils';

import graphHelper from 'legacy/graphHelper';

import $ from 'jquery';
global.jQuery = $;
require('bootstrap/js/tooltip');

const GraphFactory = {
  create(config, domNode, tooltipTitleFormatter) {
    let graph;
    switch (config.renderer) {
      case 'line':
        graph = dc.lineChart(domNode);
        D3Utils.tooltipRenderlet(graph, '.chart-body circle.dot', tooltipTitleFormatter);
        break;
      case 'area':
        graph = dc.lineChart(domNode);
        graph.renderArea(true);
        D3Utils.tooltipRenderlet(graph, '.chart-body circle.dot', tooltipTitleFormatter);
        break;
      case 'bar':
        graph = dc.barChart(domNode);
        graph.centerBar(true);
        D3Utils.tooltipRenderlet(graph, '.chart-body rect.bar', tooltipTitleFormatter);
        break;
      case 'scatterplot':
        graph = dc.lineChart(domNode);
        graph.renderDataPoints({ radius: 2, fillOpacity: 1, strokeOpacity: 1 });
        D3Utils.tooltipRenderlet(graph, '.chart-body circle.dot', tooltipTitleFormatter);
        break;
      default:
        throw new Error(`Unsupported renderer '${config.renderer}'`);
    }

    if (config.renderer === 'line' || config.renderer === 'area') {
      graph.interpolate(config.interpolation);
    }

    // Bar charts with clip padding overflow the x axis
    if (config.renderer !== 'bar') {
      graph.clipPadding(5);
    }

    return graph;
  },
};

const GraphVisualization = React.createClass({
  propTypes: {
    id: PropTypes.string.isRequired,
    data: PropTypes.object.isRequired,
    config: PropTypes.object.isRequired,
    computationTimeRange: PropTypes.object,
    height: PropTypes.number,
    width: PropTypes.number,
  },
  statics: {
    getReadableFieldChartStatisticalFunction(statisticalFunction) {
      switch (statisticalFunction) {
        case 'count':
          return 'total';
        case 'total':
          return 'sum';
        default:
          return statisticalFunction;
      }
    },
  },
  getInitialState() {
    this.triggerRender = true;
    this.graphData = crossfilter();
    this.dimension = this.graphData.dimension(d => d.x);
    this.group = this.dimension.group().reduceSum(d => d.y);

    return {
      dataPoints: [],
    };
  },
  componentDidMount() {
    this.renderGraph();
    this._updateData(this.props.data, this.props.config);
  },
  componentWillReceiveProps(nextProps) {
    if (deepEqual(this.props, nextProps)) {
      return;
    }

    if (nextProps.height !== this.props.height || nextProps.width !== this.props.width) {
      this._resizeVisualization(nextProps.width, nextProps.height);
    }
    this._updateData(nextProps.data, nextProps.config);
  },
  _updateData(data, config) {
    const isSearchAll = (config.timerange.type === 'relative' && config.timerange.range === 0);
    const dataPoints = HistogramFormatter.format(data, this.props.computationTimeRange,
      config.interval, this.props.width, isSearchAll, config.valuetype);

    this.setState({ dataPoints: this._normalizeData(dataPoints) }, this.drawData);
  },
  _normalizeData(data) {
    if (data === null || data === undefined || !Array.isArray(data)) {
      return [];
    }
    return data.map((dataPoint) => {
      dataPoint.y = NumberUtils.normalizeGraphNumber(dataPoint.y);
      return dataPoint;
    });
  },
  _formatTooltipTitle(d) {
    const formattedKey = d.x === undefined ? d.x : new DateTime(d.x).toString(DateTime.Formats.COMPLETE);

    let formattedValue;
    try {
      formattedValue = numeral(d.y).format('0,0.[00]');
    } catch (e) {
      formattedValue = d3.format('.2r')(d.y);
    }

    const valueText = `${GraphVisualization.getReadableFieldChartStatisticalFunction(this.props.config.valuetype)} ${this.props.config.field}: ${formattedValue}`;
    const keyText = `<span class="date">${formattedKey}</span>`;

    return `<div class="datapoint-info">${valueText}<br>${keyText}</div>`;
  },
  _resizeVisualization(width, height) {
    this.graph
      .width(width)
      .height(height);
    this.triggerRender = true;
  },
  drawData() {
    this.graph.xUnits(() => Math.max(this.state.dataPoints.length - 1, 1));
    this.graphData.remove();
    this.graphData.add(this.state.dataPoints);

    // Fix to make Firefox render tooltips in the right place
    // TODO: Find the cause of this
    if (this.triggerRender) {
      this.graph.render();
      this.triggerRender = false;
    } else {
      this.graph.redraw();
    }
  },
  renderGraph() {
    const graphDomNode = ReactDOM.findDOMNode(this);

    this.graph = GraphFactory.create(this.props.config, graphDomNode, this._formatTooltipTitle);
    this.graph
      .width(this.props.width)
      .height(this.props.height)
      .margins({ left: 50, right: 15, top: 10, bottom: 35 })
      .dimension(this.dimension)
      .group(this.group)
      .x(d3.time.scale())
      .elasticX(true)
      .elasticY(true)
      .renderHorizontalGridLines(true)
      .brushOn(false)
      .xAxisLabel('Time')
      .yAxisLabel(this.props.config.field)
      .renderTitle(false)
      .colors(D3Utils.glColourPalette());

    $(graphDomNode).tooltip({
      selector: '[rel="tooltip"]',
      container: 'body',
      placement: 'auto',
      delay: { show: 300, hide: 100 },
      html: true,
    });

    this.graph.xAxis()
      .ticks(graphHelper.customTickInterval())
      .tickFormat(graphHelper.customDateTimeFormat());
    this.graph.yAxis()
      .ticks(3)
      .tickFormat((value) => {
        return Math.abs(value) > 1e+30 ? value.toPrecision(1) : d3.format('.2s')(value);
      });
    this.graph.render();
  },
  render() {
    return (
      <div id={`visualization-${this.props.id}`} className={`graph ${this.props.config.renderer}`} />
    );
  },
});

export default GraphVisualization;
