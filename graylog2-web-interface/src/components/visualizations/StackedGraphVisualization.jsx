import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import Immutable from 'immutable';
import numeral from 'numeral';
import c3 from 'c3';
import d3 from 'd3';
import deepEqual from 'deep-equal';

import D3Utils from 'util/D3Utils';
import DateTime from 'logic/datetimes/DateTime';
import HistogramFormatter from 'logic/graphs/HistogramFormatter';

import graphHelper from 'legacy/graphHelper';

import style from './StackedGraphVisualization.css';

class StackedGraphVisualization extends React.Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    data: PropTypes.array.isRequired,
    height: PropTypes.number,
    width: PropTypes.number,
    config: PropTypes.object.isRequired,
    computationTimeRange: PropTypes.object,
    interactive: PropTypes.bool,
    onRenderComplete: PropTypes.func,
  };

  static defaultProps = {
    interactive: true,
    onRenderComplete: () => {},
  };

  constructor(props) {
    super(props);
    this.series = Immutable.List();
    this.seriesNames = Immutable.Map();
    this.barWidthScale = d3.scale.linear().domain(d3.range(0, 10000)).range(d3.range(0.6, 0, -0.01));
    this.dataPoints = Immutable.Set();

    this.state = {};
  }

  componentDidMount() {
    this.renderGraph(this.props);
    this.dataPoints = this._formatData(this.props);
    this.drawData();
  }

  componentWillReceiveProps(nextProps) {
    if (deepEqual(this.props, nextProps)) {
      return;
    }

    this._updateSeriesNames(nextProps);
    this.dataPoints = this._formatData(nextProps);
    if (nextProps.height !== this.props.height || nextProps.width !== this.props.width) {
      this._resizeVisualization(nextProps.width, nextProps.height);
      this.renderGraph(nextProps);
    }
    this.drawData();
  }

  _normalizeData = (data) => {
    if (data === null || data === undefined || !Array.isArray(data)) {
      return [];
    }
    return data;
  };

  _formatData = (props) => {
    const data = props.data;
    const normalizedData = this._normalizeData(data);
    const isSearchAll = (props.config.timerange.type === 'relative' && props.config.timerange.range === 0);
    const formattedSeries = [];

    normalizedData.forEach((aSeries, idx) => {
      formattedSeries.push(HistogramFormatter.format(aSeries, props.computationTimeRange,
        props.config.interval, props.width, isSearchAll, props.config.series[idx].statistical_function));
    }, this);

    return this._mergeSeries(formattedSeries);
  };

  _mergeSeries = (series) => {
    let mergedSeries = Immutable.Map();

    series.forEach((aSeries, idx) => {
      aSeries.forEach((seriesPoint) => {
        const timestamp = seriesPoint.x;
        const mergedDataPoint = Immutable.Map({ timestamp: timestamp }).set(`series${idx + 1}`, seriesPoint.y);
        if (mergedSeries.has(timestamp)) {
          mergedSeries = mergedSeries.set(timestamp, mergedSeries.get(timestamp).merge(mergedDataPoint));
        } else {
          mergedSeries = mergedSeries.set(timestamp, mergedDataPoint);
        }
      }, this);
    }, this);

    return mergedSeries.toOrderedSet().sortBy(dataPoint => dataPoint.get('timestamp'));
  };

  _getGraphType = () => {
    let graphType;

    switch (this.props.config.renderer) {
      case 'scatterplot':
        graphType = 'scatter';
        break;
      case 'line':
        graphType = (this.props.config.interpolation !== 'step-after') ? 'spline' : 'step';
        break;
      case 'area':
        graphType = (this.props.config.interpolation !== 'step-after') ? 'area-spline' : 'area-step';
        break;
      default:
        graphType = this.props.config.renderer;
    }

    return graphType;
  };

  _applyGraphConfiguration = (graphType) => {
    /* eslint-disable no-case-declarations */
    switch (graphType) {
      case 'bar':
        // Automatically resize bar width
        const numberDataPoints = this.dataPoints.size;
        this.graph.internal.config.bar_width_ratio = Math.max(0.015, this.barWidthScale(numberDataPoints));
        break;
      case 'spline':
      case 'area-spline':
        this.graph.internal.config.spline_interpolation_type = this.props.config.interpolation;
        break;
      case 'step':
      case 'area-step':
        this.graph.internal.config.line_step_type = this.props.config.interpolation;
        break;
      default:
        console.warn(`Invalid graph type ${graphType}`);
    }
    /* eslint-enable no-case-declarations */
  };

  _formatTooltipTitle = (x) => {
    return new DateTime(x).toString(DateTime.Formats.COMPLETE);
  };

  _formatTooltipValue = (value) => {
    let formattedValue;
    try {
      formattedValue = numeral(value).format('0,0.[00]');
    } catch (e) {
      formattedValue = d3.format('.2r')(value);
    }

    return formattedValue;
  };

  _resizeVisualization = (width, height) => {
    this.graph.resize({
      width: width,
      height: height,
    });
  };

  _updateSeriesNames = (props) => {
    let i = 0;
    let newSeriesNames = Immutable.Map();
    props.config.series.forEach((seriesConfig) => {
      i++;
      const seriesName = `series${i}`;
      const seriesTitle = seriesConfig.title ? seriesConfig.title : `${seriesConfig.statistical_function} ${seriesConfig.field}, "${seriesConfig.query || '*'}"`;
      newSeriesNames = newSeriesNames.set(seriesName, seriesTitle);
    }, this);

    if (!Immutable.is(this.seriesNames, newSeriesNames)) {
      this.seriesNames = newSeriesNames;
      this.graph.data.names(this.seriesNames.toJS());
    }
  };

  drawData = () => {
    const graphType = this._getGraphType();
    this._applyGraphConfiguration(graphType);

    if (!this.dataPoints.isEmpty()) {
      // Generate custom tick values for the time axis
      this.graph.internal.config.axis_x_tick_values = graphHelper.customTickInterval()(
        this.dataPoints.first().get('timestamp') - 1000,
        this.dataPoints.last().get('timestamp') + 1000,
      );
    }

    this.graph.load({
      json: this.dataPoints.toJS(),
      keys: {
        x: 'timestamp',
        value: this.series.toJS(),
      },
      type: graphType,
    });
  };

  renderGraph = (props) => {
    const graphDomNode = this._graph;
    const colourPalette = D3Utils.glColourPalette();

    let i = 0;
    let colours = Immutable.Map();

    props.config.series.forEach((seriesConfig) => {
      i++;
      const seriesName = `series${i}`;
      const seriesTitle = seriesConfig.title ? seriesConfig.title : `${seriesConfig.statistical_function} ${seriesConfig.field}, "${seriesConfig.query || '*'}"`;
      this.series = this.series.push(seriesName);
      this.seriesNames = this.seriesNames.set(seriesName, seriesTitle);
      colours = colours.set(seriesName, colourPalette(seriesName));
    });

    this.yAxisFormatter = (value) => {
      return Math.abs(value) > 1e+30 || value === 0 ? value.toPrecision(1) : d3.format('.2s')(value);
    };

    const config = {
      bindto: graphDomNode,
      onrendered: this.props.onRenderComplete,
      size: {
        height: props.height,
        width: props.width,
      },
      data: {
        columns: [],
        names: this.seriesNames.toJS(),
        colors: colours.toJS(),
      },
      padding: {
        right: 10,
      },
      axis: {
        x: {
          type: 'timeseries',
          label: {
            text: 'Time',
            position: 'outer-center',
          },
          tick: {
            format: graphHelper.customDateTimeFormat(),
          },
        },
        y: {
          label: {
            text: 'Values',
            position: 'outer-middle',
          },
          tick: {
            count: 3,
            format: this.yAxisFormatter,
          },
          padding: {
            bottom: 0,
          },
        },
      },
      grid: {
        y: {
          show: true,
          ticks: 3,
        },
        focus: {
          show: false,
        },
      },
      tooltip: {
        format: {
          title: this._formatTooltipTitle,
          value: this._formatTooltipValue,
        },
      },
    };

    if (!this.props.interactive) {
      config.interaction = { enabled: false };
      config.transition = { duration: null };
    }

    this.graph = c3.generate(config);
  };

  render() {
    const classNames = this.props.config.doNotShowCircles ? 'donotshowcircles' : '';
    return (
      <div ref={(c) => { this._graph = c; }} id={`visualization-${this.props.id}`}
           className={`graph ${this.props.config.renderer}${classNames}`} />
    );
  }
}

export default StackedGraphVisualization;
