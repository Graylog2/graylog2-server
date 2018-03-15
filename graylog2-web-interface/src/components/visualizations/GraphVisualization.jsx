import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import numeral from 'numeral';
import crossfilter from 'crossfilter';
import dc from 'dc';
import d3 from 'd3';
import deepEqual from 'deep-equal';
import _ from 'lodash';

import DateTime from 'logic/datetimes/DateTime';
import HistogramFormatter from 'logic/graphs/HistogramFormatter';

import D3Utils from 'util/D3Utils';
import NumberUtils from 'util/NumberUtils';

import graphHelper from 'legacy/graphHelper';

import $ from 'jquery';
global.jQuery = $;
require('bootstrap/js/tooltip');

const GraphFactory = {
  create(config, domNode, renderTooltip, tooltipTitleFormatter) {
    let graph;
    let tooltipSelector;
    switch (config.renderer) {
      case 'line':
        graph = dc.lineChart(domNode);
        tooltipSelector = '.chart-body circle.dot';
        break;
      case 'area':
        graph = dc.lineChart(domNode);
        graph.renderArea(true);
        tooltipSelector = '.chart-body circle.dot';
        break;
      case 'bar':
        graph = dc.barChart(domNode);
        graph.centerBar(true);
        tooltipSelector = '.chart-body rect.bar';
        break;
      case 'scatterplot':
        graph = dc.lineChart(domNode);
        graph.renderDataPoints({ radius: 2, fillOpacity: 1, strokeOpacity: 1 });
        tooltipSelector = '.chart-body circle.dot';
        break;
      default:
        throw new Error(`Unsupported renderer '${config.renderer}'`);
    }

    if (renderTooltip && tooltipSelector) {
      D3Utils.tooltipRenderlet(graph, tooltipSelector, tooltipTitleFormatter);
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

class GraphVisualization extends React.Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    data: PropTypes.object.isRequired,
    config: PropTypes.object.isRequired,
    computationTimeRange: PropTypes.shape({
      from: PropTypes.string.isRequired,
      to: PropTypes.string.isRequired,
    }).isRequired,
    height: PropTypes.number,
    width: PropTypes.number,
    interactive: PropTypes.bool,
    onRenderComplete: PropTypes.func,
  };

  static getReadableFieldChartStatisticalFunction(statisticalFunction) {
    switch (statisticalFunction) {
      case 'count':
        return 'total';
      case 'total':
        return 'sum';
      default:
        return statisticalFunction;
    }
  }

  static defaultProps = {
    interactive: true,
    onRenderComplete: () => {},
  };

  constructor(props) {
    super(props);
    this.triggerRender = true;
    this.graphData = crossfilter();
    this.dimension = this.graphData.dimension(d => d.x);
    this.group = this.dimension.group().reduceSum(d => d.y);

    this.state = {
      dataPoints: [],
    };
  }

  componentDidMount() {
    this.disableTransitions = dc.disableTransitions;
    dc.disableTransitions = !this.props.interactive;
    this.renderGraph();
    this._updateData(this.props.data, this.props.config, this.props.computationTimeRange);
  }

  componentDidUpdate() {
    this.drawData();
  }

  componentWillReceiveProps(nextProps) {
    if (deepEqual(this.props, nextProps)) {
      return;
    }

    if (nextProps.height !== this.props.height || nextProps.width !== this.props.width) {
      this._resizeVisualization(nextProps.width, nextProps.height);
    }
    this._updateData(nextProps.data, nextProps.config, nextProps.computationTimeRange);
  }

  componentWillUnmount() {
    dc.disableTransitions = this.disableTransitions;
  }

  _updateData = (data, config, computationTimeRange) => {
    const isSearchAll = (config.timerange.type === 'relative' && config.timerange.range === 0);
    const dataPoints = HistogramFormatter.format(data, computationTimeRange,
      config.interval, this.props.width, isSearchAll, config.valuetype);

    this.setState({ dataPoints: this._normalizeData(dataPoints) });
  };

  _normalizeData = (data) => {
    if (data === null || data === undefined || !Array.isArray(data)) {
      return [];
    }
    return data.map((dataPoint) => {
      dataPoint.y = NumberUtils.normalizeGraphNumber(dataPoint.y);
      return dataPoint;
    });
  };

  _formatTooltipTitle = (d) => {
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
  };

  _resizeVisualization = (width, height) => {
    this.graph
      .width(width)
      .height(height);
    this.triggerRender = true;
  };

  drawData = () => {
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
    if (this.props.config.threshold) {
      this.renderThreshold();
    }
  };

  // Draws a horizontal threshold line in the graph
  renderThreshold = () => {
    const threshold = this.props.config.threshold;
    const thresholdColor = this.props.config.threshold_color || '#f00';
    const thresholdTooltip = this.props.config.threshold_tooltip || `threshold: ${threshold}`;
    const thresholdDotHidden = 1e-6;
    const thresholdDotVisible = 0.8;
    const thresholdDotRadius = 4;

    this.graph.on('renderlet.threshold', (chart) => {
      const lineData = [
        {
          x: chart.x().range()[0],
          y: chart.y()(threshold),
        },
        {
          x: chart.x().range()[1],
          y: chart.y()(threshold),
        },
      ];

      const line = d3.svg.line()
        .x(d => d.x)
        .y(d => d.y)
        .interpolate('linear');

      const chartBody = chart.select('g.chart-body');
      const paths = chartBody.selectAll('path.threshold').data([lineData]);
      paths // Modify the existing path
        .attr('stroke', thresholdColor)
        .attr('d', line);
      paths.enter() // This will only do something if there isn't a path yet
        .append('path')
        .attr('class', 'threshold')
        .attr('stroke', thresholdColor)
        .attr('stroke-width', 1)
        .attr('stroke-dasharray', ('2', '2'))
        .attr('d', line);
      paths.exit().remove(); // Remove any outdated paths

      // Collect all x-axis values
      const xValues = chart.data().reduce((list, value) => {
        value.values.forEach(v => list.push(v.x));
        return list;
      }, []);

      // Put circles on the threshold line to make it easier to show the tooltip
      const dots = chartBody.selectAll('circle.threshold').data(xValues);
      dots.enter()
        .append('circle')
        .attr('class', 'threshold')
        .attr('r', thresholdDotRadius)
        .attr('rel', 'tooltip') // To make the tooltip helper pick it up
        .attr('data-original-title', () => {
          return `<div class="datapoint-info">${_.escape(thresholdTooltip)}</div>`;
        })
        .style('stroke-opacity', thresholdDotHidden)
        .style('fill-opacity', thresholdDotHidden)
        .on('mousemove', function show() {
          d3.select(this)
            .style('stroke-opacity', thresholdDotVisible)
            .style('fill-opacity', thresholdDotVisible);
        })
        .on('mouseout', function hide() {
          d3.select(this)
            .style('stroke-opacity', thresholdDotHidden)
            .style('fill-opacity', thresholdDotHidden);
        });
      dots
        .attr('cx', d => dc.utils.safeNumber(chart.x()(d)))
        .attr('cy', () => dc.utils.safeNumber(chart.y()(threshold)))
        .attr('data-original-title', () => {
          return `<div class="datapoint-info">${thresholdTooltip}</div>`;
        })
        .attr('fill', thresholdColor);
      dots.exit().remove();
    });
  };

  renderGraph = () => {
    const graphDomNode = this._graph;
    const interactive = this.props.interactive;

    this.graph = GraphFactory.create(this.props.config, graphDomNode, interactive, this._formatTooltipTitle);
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

    this.graph.on('postRender', this.props.onRenderComplete);

    if (interactive) {
      $(graphDomNode).tooltip({
        selector: '[rel="tooltip"]',
        container: 'body',
        placement: 'auto',
        delay: { show: 300, hide: 100 },
        html: true,
      });
    }

    this.graph.xAxis()
      .ticks(graphHelper.customTickInterval())
      .tickFormat(graphHelper.customDateTimeFormat());
    this.graph.yAxis()
      .ticks(3)
      .tickFormat((value) => {
        return Math.abs(value) > 1e+30 ? value.toPrecision(1) : d3.format('.2s')(value);
      });
    this.graph.render();
  };

  render() {
    return (
      <div ref={(c) => { this._graph = c; }} id={`visualization-${this.props.id}`}
           className={`graph ${this.props.config.renderer}`} />
    );
  }
}

export default GraphVisualization;
