import jQuery from 'jquery';
import {} from 'jquery-ui/ui/widgets/draggable';
import {} from 'jquery-ui/ui/widgets/droppable';
import moment from 'moment';
import numeral from 'numeral';
import Rickshaw from 'rickshaw';
import Graylog2Time from 'legacy/Rickshaw.Fixtures.Graylog2Time';
import Graylog2Selector from 'legacy/Rickshaw.Graph.Graylog2Selector';

import DateTime from 'logic/datetimes/DateTime';
import GraphVisualization from 'components/visualizations/GraphVisualization';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import StringUtils from 'util/StringUtils';
import HistogramFormatter from 'logic/graphs/HistogramFormatter';

import StoreProvider from 'injection/StoreProvider';
const SearchStore = StoreProvider.getStore('Search');

function generateShortId() {
  return Math.random().toString(36).substr(2, 9);
}

export function generateId() {
  let r = '';
  for (let i = 0; i < 4; i++) {
    r += generateShortId();
  }

  return r;
}

function sendFailureEvent(graphId, errorMessage) {
  jQuery(document).trigger('failed.graylog.fieldgraph', { graphId: graphId, errorMessage: errorMessage });
}

function sendCreatedGraphEvent(opts) {
  jQuery(document).trigger('created.graylog.fieldgraph', { graphOptions: opts });
}

function sendUpdatedGraphEvent(opts) {
  jQuery(document).trigger('updated.graylog.fieldgraph', { graphOptions: opts });
}

function sendMergedGraphsEvent(targetGraphId, draggedGraphId) {
  jQuery(document).trigger('merged.graylog.fieldgraph', { targetGraphId: targetGraphId, draggedGraphId: draggedGraphId });
}

export const FieldChart = {
  fieldGraphs: {},
  GRAPH_HEIGHT: 120,
  palette: new Rickshaw.Color.Palette({ scheme: 'colorwheel' }),

  reload() {
    this.palette = new Rickshaw.Color.Palette({ scheme: 'colorwheel' });
  },

  _getDefaultOptions(opts) {
    const searchParams = SearchStore.getOriginalSearchParams().toJS();

    // Options.
    if (opts.chartid === undefined) {
      opts.chartid = generateId();
    }

    if (opts.interval === undefined) {
      opts.interval = searchParams.interval || 'minute';
    }

    if (opts.interpolation === undefined) {
      opts.interpolation = 'linear';
    }

    if (opts.renderer === undefined) {
      opts.renderer = 'bar';
    }

    if (opts.valuetype === undefined) {
      opts.valuetype = 'mean';
    }

    if (opts.query === undefined) {
      opts.query = searchParams.query;
    }

    if (opts.createdAt === undefined) {
      opts.createdAt = moment().valueOf();
    }

    // Always get stream and time range params from the current search request.
    // Ignore the streamid, rangetype and range params that have been stored in localStorage. (see FieldGraphsStore)
    opts.streamid = searchParams.streamId;
    opts.rangetype = searchParams.range_type;
    opts.range = {};

    switch (opts.rangetype) {
      case 'relative':
        opts.range.relative = searchParams.relative;
        break;
      case 'absolute':
        opts.range.from = searchParams.from;
        opts.range.to = searchParams.to;
        break;
      case 'keyword':
        opts.range.keyword = searchParams.keyword;
        break;
      default:
    }

    return opts;
  },

  _getTimeRangeParams(opts) {
    const timerange = {};
    switch (opts.rangetype) {
      case 'relative':
        timerange.range = opts.range.relative;
        break;
      case 'absolute':
        timerange.from = opts.range.from;
        timerange.to = opts.range.to;
        break;
      case 'keyword':
        timerange.keyword = opts.range.keyword;
        break;
      default:
    }

    return timerange;
  },

  _onFieldHistogramLoad($graphContainer, $graphElement, $graphYAxis, opts, data) {
    if (opts.query.trim().length > 0) {
      jQuery('.field-graph-query', $graphContainer).text(opts.query);
    } else {
      jQuery('.field-graph-query', $graphContainer).text('*');
    }

    const lines = [];
    lines.push(JSON.stringify(opts));
    $graphContainer.attr('data-lines', lines);

    this._updateStatisticalFunctionText($graphContainer, opts);

    // Do not add from time when we search in all messages
    const from = $graphContainer.data('from') !== undefined ? data.from : undefined;

    const graph = new Rickshaw.Graph({
      element: $graphElement[0],
      width: $graphElement.width(),
      height: this.GRAPH_HEIGHT,
      interpolation: opts.interpolation,
      renderer: opts.renderer,
      resolution: data.interval,
      series: [{
        name: opts.chartid,
        data: data.values,
        color: '#26ADE4',
        gl2_query: opts.query,
        valuetype: GraphVisualization.getReadableFieldChartStatisticalFunction(opts.valuetype),
        field: opts.field,
      }],
    });

    new Rickshaw.Graph.Axis.Y({
      graph: graph,
      tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
      orientation: 'left',
      element: $graphYAxis[0],
      pixelsPerTick: 30,
    });

    new Rickshaw.Graph.Axis.Time({
      graph: graph,
      ticksTreatment: 'glow',
      timeFixture: new Rickshaw.Fixtures.Graylog2Time(), // Cares about correct TZ handling.
    });

    new Rickshaw.Graph.HoverDetail({
      graph: graph,
      formatter(series, x, y) {
        const date = `<span class="date">${new DateTime(x * 1000).toString(DateTime.Formats.COMPLETE)}</span>`;
        const swatch = `<span class="detail_swatch" style="background-color: ${series.color}"></span>`;
        const content = `${swatch}[${series.valuetype}] ${series.field}: ${numeral(y).format('0,0.[000]')}<br>${date}`;
        return content;
      },
    });

    new Rickshaw.Graph.Graylog2Selector({
      graph: graph,
    });

    if (opts.renderer === 'scatterplot') {
      graph.renderer.dotSize = 2;
    }

    if (opts.renderer === 'area') {
      graph.renderer.stroke = true;
    }

    graph.render();

    this.fieldGraphs[opts.chartid] = graph;

    // Make it draggable.
    $graphContainer.draggable({
      handle: '.reposition-handle',
      cursor: 'move',
      scope: '#field-graphs',
      revert: 'invalid', // return to position when not dropped on a droppable.
      opacity: 0.5,
      containment: jQuery('#field-graphs'),
      axis: 'y',
      snap: jQuery('.field-graph-container'),
      snapMode: 'inner',
    });

    const that = this;

    // ...and droppable.
    $graphContainer.droppable({
      scope: '#field-graphs',
      tolerance: 'intersect',
      activate(event, ui) {
        // Show merge hints on all charts except the dragged one when dragging starts.
        jQuery('#field-graphs .merge-hint').not(jQuery('.merge-hint', ui.draggable)).show();
      },
      deactivate() {
        // Hide all merge hints when dragging stops.
        jQuery('#field-graphs .merge-hint').hide();
      },
      over() {
        jQuery(this).css('background-color', '#C7E2ED');
        jQuery('.merge-hint span', jQuery(this)).switchClass('alpha80', 'merge-drop-ready');
      },
      out() {
        jQuery(this).css('background-color', '#fff');
        jQuery('.merge-hint span', jQuery(this)).switchClass('merge-drop-ready', 'alpha80');
      },
      drop(event, ui) {
        // Merge charts.
        const target = jQuery(this).attr('data-chart-id');
        const dragged = ui.draggable.attr('data-chart-id');

        ui.draggable.hide();
        jQuery(this).css('background-color', '#fff');

        that._mergeCharts(target, dragged);
      },
    });
  },

  _onFieldHistogramFail($graphElement, opts, error) {
    if (error.additional && error.additional.status === 400) {
      sendFailureEvent(opts.chartid, 'Field graphs are only available for numeric fields.');
    } else {
      const alert = jQuery('<div>').addClass('alert').addClass('alert-warning').text('Field graph could not be loaded, please try again after reloading the page.');
      $graphElement.html(alert);
      const errorMessage = (error.additional ? ` with status ${error.additional.status}` : ` with error: ${error.message}`);
      UserNotification.error(`Loading field graph for '${opts.field}' failed ${errorMessage}`);
      console.error(error);
    }
  },

  _chartOptionsFromContainer(cc) {
    try {
      return JSON.parse(cc.attr('data-lines'));
    } catch (e) {
      return this._getDefaultOptions();
    }
  },

  _changeGraphConfig(graphContainer, key, value) {
    const options = this._chartOptionsFromContainer(graphContainer);
    options[key] = value;

    graphContainer.attr('data-lines', JSON.stringify(options));
    sendUpdatedGraphEvent(options);
  },

  _insertSpinner($graphContainer) {
    const spinnerElement = jQuery(`<div class="spinner" style="height: ${this.GRAPH_HEIGHT}px; line-height: ${this.GRAPH_HEIGHT}px;"><i class="fa fa-spin fa-refresh fa-3x spinner"></i></div>`);
    $graphContainer.append(spinnerElement);
  },

  _deleteSpinner($graphContainer) {
    jQuery('.spinner', $graphContainer).remove();
  },

  _fetchData(opts, timeRangeParams) {
    const url = ApiRoutes.UniversalSearchApiController.fieldHistogram(
      opts.rangetype,
      opts.query || '*',
      opts.field,
      opts.interval,
      timeRangeParams,
      opts.streamid,
      opts.valuetype === 'cardinality',
    ).url;

    return fetch('GET', URLUtils.qualifyUrl(url))
      .then((response) => {
        const formattedResponse = {
          time: response.time,
          interval: response.interval,
          from: response.queried_timerange.from,
          to: response.queried_timerange.to,
        };

        formattedResponse.values = HistogramFormatter.format(response.results, response.queried_timerange, opts.interval,
          jQuery(window).width(), opts.rangetype === 'relative' && opts.range.relative === 0, opts.valuetype, true);

        return formattedResponse;
      });
  },

  renderFieldChart(opts, graphContainer, options) {
    const renderingOptions = options || {};
    const field = opts.field;
    const $graphContainer = jQuery(graphContainer);
    const $graphElement = jQuery('.field-graph', $graphContainer);
    const $graphYAxis = jQuery('.field-graph-y-axis', $graphContainer);

    if (renderingOptions.newGraph) {
      this._insertSpinner($graphContainer);
    }

    opts = this._getDefaultOptions(opts);
    const timeRangeParams = this._getTimeRangeParams(opts);

    const promise = this._fetchData(opts, timeRangeParams);
    promise
      .then((data) => {
        // Delete a possibly already existing graph to manage updates.
        $graphElement.html('');
        $graphYAxis.html('');
        $graphYAxis.hide();

        this._onFieldHistogramLoad($graphContainer, $graphElement, $graphYAxis, opts, data);

        if (renderingOptions.newGraph) {
          sendCreatedGraphEvent(opts);
        }
      })
      .catch(error => this._onFieldHistogramFail($graphElement, opts, error))
      .finally(() => {
        $graphYAxis.show();
        if (renderingOptions.newGraph) {
          this._deleteSpinner($graphContainer);
        }
      });
  },

  updateFieldChartData(graphId, options, seriesName) {
    options = this._getDefaultOptions(options);
    const timeRangeParams = this._getTimeRangeParams(options);

    const promise = this._fetchData(options, timeRangeParams);
    promise.then(
      (data) => {
        const fieldGraph = this.fieldGraphs[graphId];
        if (fieldGraph) {
          const series = fieldGraph.series.filter(aSeries => aSeries.name === seriesName)[0];
          if (series) {
            series.valuetype = GraphVisualization.getReadableFieldChartStatisticalFunction(options.valuetype);
            series.data = data.values;
            fieldGraph.update();
          }
        }
      },
      (error) => {
        UserNotification.error(`Updating field graph data failed: ${error}`, 'Could not update field graph data');
      },
    );
  },

  _updateStatisticalFunctionText($graphContainer, graphOptions) {
    jQuery('.type-description', $graphContainer).text(`[${GraphVisualization.getReadableFieldChartStatisticalFunction(graphOptions.valuetype)}] ${graphOptions.field}, `);
  },

  renderNewFieldChart(options, graphContainer) {
    this.renderFieldChart(options, graphContainer, { newGraph: true });
  },

  changeInterpolation(graphContainer, interpolation) {
    const graphOptions = this._chartOptionsFromContainer(graphContainer);
    this._changeGraphConfig(graphContainer, 'interpolation', interpolation);

    const graph = this.fieldGraphs[graphOptions.chartid];
    graph.interpolation = interpolation;
    graph.render();
  },

  changeRenderer(graphContainer, renderer) {
    const graphOptions = this._chartOptionsFromContainer(graphContainer);
    this._changeGraphConfig(graphContainer, 'renderer', renderer);

    const graph = this.fieldGraphs[graphOptions.chartid];
    graph.setRenderer(renderer);

    if (renderer === 'scatterplot') {
      graph.renderer.dotSize = 2;
    }

    if (renderer === 'area') {
      graph.renderer.stroke = true;
    }

    graph.renderer.unstack = true;
    graph.render();
  },

  changeResolution(graphContainer, resolution) {
    const graphOptions = this._chartOptionsFromContainer(graphContainer);
    graphOptions.interval = resolution;
    this._changeGraphConfig(graphContainer, 'interval', resolution);
    this.updateFieldChartData(graphOptions.chartid, graphOptions, graphOptions.chartid);
  },

  changeStatisticalFunction($graphContainer, statisticalFunction) {
    const graphOptions = this._chartOptionsFromContainer($graphContainer);
    graphOptions.valuetype = statisticalFunction;
    this._changeGraphConfig($graphContainer, 'valuetype', statisticalFunction);
    this.updateFieldChartData(graphOptions.chartid, graphOptions, graphOptions.chartid);
    this._updateStatisticalFunctionText($graphContainer, graphOptions);
  },

  _mergeCharts(targetId, draggedId) {
    const targetChart = this.fieldGraphs[targetId];
    const draggedChart = this.fieldGraphs[draggedId];

    const targetElem = jQuery(`.field-graph-container[data-chart-id='${targetId}']`);

    for (let i = 0; i < draggedChart.series.length; i++) {
      const lineColor = this.palette.color();
      const series = draggedChart.series[i];
      let query = series.gl2_query;

      if (query === undefined || query === '') {
        query = '*';
      }

      // Add query to query list of chart.
      const queryDescription = `<div class="field-graph-query-color" style="background-color: "${lineColor};"></div> `
        + `<span class="type-description">[${StringUtils.escapeHTML(series.valuetype)}] ${series.field}, </span> `
        + `Query: <span class="field-graph-query">${StringUtils.escapeHTML(query)}</span>`;

      jQuery('ul.field-graph-query-container', targetElem).append(`<li>${queryDescription}</li>`);

      const addSeries = {
        name: series.name,
        color: lineColor,
        gl2_query: query,
        valuetype: series.valuetype,
        field: series.field,
      };

      addSeries.data = series.data;

      targetChart.series.push(addSeries);
    }

    targetChart.renderer.unstack = true;

    sendMergedGraphsEvent(targetId, draggedId);

    // Reflect all the chart changes we made.
    targetChart.update();
    targetChart.render();
  },

  stackGraphs(targetGraphId, sourceGraphId) {
    this._mergeCharts(targetGraphId, sourceGraphId);
    const sourceGraphElement = jQuery(`.field-graph-container[data-chart-id="${sourceGraphId}"]`);
    sourceGraphElement.hide();
  },

  redraw(graphId) {
    const graph = this.fieldGraphs[graphId];
    if (graph) {
      const $graphContainer = jQuery(`.field-graph-container[data-chart-id="${graphId}"]`);
      const $graphElement = jQuery('.field-graph', $graphContainer);
      graph.configure({ width: $graphElement.width() });
      graph.render();
    }
  },
};

// Changing type of value graphs.
jQuery(document).on('click', '.field-graph-container ul.renderer-selector li a', function (e) {
  e.preventDefault();

  const graphContainer = jQuery(this).closest('.field-graph-container');
  const type = jQuery(this).attr('data-type');
  FieldChart.changeRenderer(graphContainer, type);
});

// Changing interpolation of value graphs.
jQuery(document).on('click', '.field-graph-container ul.interpolation-selector li a', function (e) {
  e.preventDefault();

  const graphContainer = jQuery(this).closest('.field-graph-container');
  const interpolation = jQuery(this).attr('data-type');
  FieldChart.changeInterpolation(graphContainer, interpolation);
});

// Changing interval of value graphs.
jQuery(document).on('click', '.field-graph-container ul.interval-selector li a', function (e) {
  e.preventDefault();

  const graphContainer = jQuery(this).closest('.field-graph-container');
  const interval = jQuery(this).attr('data-type');
  FieldChart.changeResolution(graphContainer, interval);
});

// Changing value type of value graphs.
jQuery(document).on('click', '.field-graph-container ul.valuetype-selector li a', function (e) {
  e.preventDefault();

  const graphContainer = jQuery(this).closest('.field-graph-container');
  const valuetype = jQuery(this).attr('data-type');
  FieldChart.changeStatisticalFunction(graphContainer, valuetype);
});
