import $ from 'jquery';
import Rickshaw from 'rickshaw';
import DateTime from 'logic/datetimes/DateTime';
import AlertsAnnotator from 'logic/alerts/AlertsAnnotator';
import Graylog2Time from 'legacy/Rickshaw.Fixtures.Graylog2Time';
import Graylog2Selector from 'legacy/Rickshaw.Graph.Graylog2Selector';
import numeral from 'numeral';

const resultHistogram = {
  _histogram: [],
  _histogramContainer: $('#result-graph'),
  _yAxis: $('#y_axis'),
  _graphTimeline: $('#result-graph-timeline'),
  _annotator: undefined,
  _resultHistogramGraph: undefined,

  _getHistogramContainerWidth() {
    return this._histogramContainer.width();
  },

  resetContainerElements(elem) {
    this._resultHistogramGraph = undefined;
    this._histogramContainer = $('#result-graph', elem);
    this._yAxis = $('#y_axis', elem);
    this._graphTimeline = $('#result-graph-timeline', elem);
  },

  setData(data, stream) {
    this._histogram = data;
    this._stream = stream;
  },

  drawResultGraph() {
    if (this._histogramContainer.length === 0) {
      return;
    }

    if (typeof this._resultHistogramGraph !== 'undefined') {
      return;
    }

    this._histogramContainer.html('');
    this._yAxis.html('');
    this._graphTimeline.html('');

    const selectedResolution = $('.date-histogram-res-selector.selected-resolution').data('resolution');

    const resultGraph = new Rickshaw.Graph({
      element: this._histogramContainer[0],
      width: this._getHistogramContainerWidth(),
      height: 120,
      renderer: 'bar',
      resolution: selectedResolution,
      series: [{
        name: 'Messages',
        data: this._histogram,
        color: '#26ADE4',
      }],
    });

    new Rickshaw.Graph.Axis.Y({
      graph: resultGraph,
      tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
      orientation: 'left',
      element: this._yAxis[0],
      pixelsPerTick: 30,
    });

        // Only show a x-axis (time) when there is more than one bucket.
    if (resultGraph.series != undefined && resultGraph.series[0] != undefined && resultGraph.series[0].data.length > 1) {
      new Rickshaw.Graph.Axis.Time({
        graph: resultGraph,
        ticksTreatment: 'glow',
        timeFixture: new Rickshaw.Fixtures.Graylog2Time(), // Cares about correct TZ handling.
      });
    }

    new Rickshaw.Graph.HoverDetail({
      graph: resultGraph,
      formatter(series, x, y) {
        const dateString = new DateTime(x * 1000).toString(DateTime.Formats.COMPLETE);
        const date = `<span class="date">${dateString}</span>`;
        const swatch = '<span class="detail_swatch"></span>';
        const content = `${numeral(parseInt(y)).format('0,0')} messages<br>${date}`;
        return content;
      },
      xFormatter(x) {
        return new Date(x * 1000).toDateString();
      },
    });

    new Rickshaw.Graph.Graylog2Selector({
      graph: resultGraph,
    });

    this._annotator = new Rickshaw.Graph.Annotate({
      graph: resultGraph,
      element: this._graphTimeline[0],
    });

    AlertsAnnotator.fillAlertAnnotator(this._histogram, this._stream, this._annotator);

    resultGraph.render();

    this._resultHistogramGraph = resultGraph;
  },

  updateData(newData) {
    if (this._histogram.length > 0) {
      if (typeof this._resultHistogramGraph !== 'undefined') {
        this._histogram = newData;
        this._resultHistogramGraph.series[0].data = newData;
        this._resetAlertAnnotator();
        this._resultHistogramGraph.update();
      }
    }
  },

    // I'm really sorry about this, but I can't figure out a better way of refreshing the annotator without flickering
  _resetAlertAnnotator() {
    const $oldAnnotations = $('.content', this._graphTimeline);

    AlertsAnnotator.fillAlertAnnotator(this._histogram, this._stream, this._annotator, () => {
      $oldAnnotations.remove();
    });
  },

  redrawResultGraph() {
    if (this._histogram.length > 0) {
      if (typeof this._resultHistogramGraph !== 'undefined') {
        this._resultHistogramGraph.configure({ width: this._getHistogramContainerWidth() });
        this._resultHistogramGraph.render();
      }
    }
  },
};

export default resultHistogram;
