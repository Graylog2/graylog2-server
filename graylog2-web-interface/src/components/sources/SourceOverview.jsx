import React from 'react';
import $ from 'jquery';
import crossfilter from 'crossfilter';
import dc from 'dc';
import Qs from 'qs';
import moment from 'moment';

import SourceDataTable from './SourceDataTable';
import SourcePieChart from './SourcePieChart';
import SourceLineChart from './SourceLineChart';
import SupportLink from 'components/support/SupportLink';
import { Spinner } from 'components/common';

import DateTime from 'logic/datetimes/DateTime';
import UniversalSearch from 'logic/search/UniversalSearch';
import EventHandlersThrottler from 'util/EventHandlersThrottler';

import StoreProvider from 'injection/StoreProvider';
const SourcesStore = StoreProvider.getStore('Sources');
const HistogramDataStore = StoreProvider.getStore('HistogramData');
const SearchStore = StoreProvider.getStore('Search');

import ActionsProvider from 'injection/ActionsProvider';
const HistogramDataActions = ActionsProvider.getActions('HistogramData');

const daysToSeconds = days => moment.duration(days, 'days').as('seconds');
const hoursToSeconds = hours => moment.duration(hours, 'hours').as('seconds');

const DEFAULT_RANGE_IN_SECS = hoursToSeconds(1);
const SUPPORTED_RANGES_IN_SECS = [hoursToSeconds(1), daysToSeconds(1), daysToSeconds(7), daysToSeconds(31), daysToSeconds(365), 0];

let SCREEN_RESOLUTION = $(window).width();

const SourceOverview = React.createClass({
  getInitialState() {
    this.sourcesData = crossfilter();
    this.filterDimension = this.sourcesData.dimension(d => d.name);
    this.nameDimension = this.sourcesData.dimension(d => d.name);
    this.nameMessageGroup = this.nameDimension.group().reduceSum(d => d.message_count);

    this.messageCountDimension = this.sourcesData.dimension(d => d.message_count);

    this.histogramData = crossfilter();
    this.valueDimension = this.histogramData.dimension(d => new Date(d.x));
    this.valueGroup = this.valueDimension.group().reduceSum(d => d.y);

    return {
      range: null,
      resolution: 'minute',
      filter: '',
      loading: false,
      renderResultTable: true,
      histogramDataAvailable: true,
      reloadingHistogram: false,
    };
  },

  componentDidMount() {
    const onDataTableFiltered = (sourceName) => {
      this.refs.sourcePieChart.setFilter(sourceName);
      this._toggleResetButtons();
      dc.redrawAll();
      this.loadHistogramData();
    };

    const onPieChartFiltered = () => {
      this.loadHistogramData();
      this._toggleResetButtons();
    };

    const onLineChartFiltered = (filter) => {
      if (filter) {
        SearchStore.changeTimeRange('absolute', {
          from: new DateTime(filter[0]).toString(),
          to: new DateTime(filter[1]).toString(),
        });
      } else {
        this.syncRangeWithQuery();
      }
    };

    this.refs.sourceDataTable.renderDataTable(this.messageCountDimension, this.nameMessageGroup, onDataTableFiltered);
    this.refs.sourcePieChart.renderPieChart(this.nameDimension, this.nameMessageGroup, onPieChartFiltered);
    this.refs.sourceLineChart.renderLineChart(this.valueDimension, this.valueGroup, onLineChartFiltered);
    this.applyRangeParameter();
    dc.renderAll();
    window.addEventListener('resize', this._resizeCallback);
    $(window).on('hashchange', this._applyRangeFromHash);
    // register them live as we do not know if those buttons are currently in the DOM
    $(document).on('click', '.sidebar-hide', this._updateWidth);
    $(document).on('click', '.sidebar-show', this._updateWidth);
    UniversalSearch.init();
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._resizeCallback);
    $(window).off('hashchange', this._applyRangeFromHash);
    $(document).off('click', '.sidebar-hide', this._updateWidth);
    $(document).off('click', '.sidebar-show', this._updateWidth);
  },

  setSearchFilter(filter) {
    this.setState({ filter: filter }, () => {
      this._filterSources();
      this.refs.sourceDataTable.redraw();
      this.refs.sourcePieChart.redraw();
    });
  },

  NUMBER_OF_TOP_VALUES: 10,
  eventThrottler: new EventHandlersThrottler(),

  loadData() {
    this.loadSources();
    this.loadHistogramData();
  },

  loadSources() {
    const onLoaded = (sources) => {
      this._resetSources(sources);
      this.setState({ renderResultTable: this.sourcesData.size() !== 0, loading: false });
      this._updateWidth();
    };

    SourcesStore.loadSources(this.state.range, onLoaded);
  },

  _resetSources(sources) {
    /*
     * http://stackoverflow.com/questions/23500546/replace-crossfilter-data-restore-dimensions-and-groups
     * It looks like dc interacts with crossfilter to represent the graphs and apply some filters
     * on the crossfilter dimension, but it also stores those filters internally. That means that
     * we need to remove the dimension and graphs filters, but we only need to reapply filters to the
     * graphs, dc will propagate that to the crossfilter dimension.
     */
    const pieChartFilters = this.refs.sourcePieChart.getFilters();
    const dataTableFilters = this.refs.sourceDataTable.getFilters();
    this.nameDimension.filterAll();
    this.filterDimension.filterAll();
    this.refs.sourcePieChart.clearFilters();
    this.refs.sourceDataTable.clearFilters();
    this.sourcesData.remove();
    this.sourcesData.add(sources);

    pieChartFilters.forEach(filter => this.refs.sourcePieChart.setFilter(filter));
    dataTableFilters.forEach(filter => this.refs.sourceDataTable.setFilter(filter));
    this._filterSources();

    dc.redrawAll();
  },

  loadHistogramData() {
    let filters;

    if (this.refs.sourcePieChart.getFilters().length !== 0 || this.refs.sourceDataTable.getFilters().length !== 0) {
      filters = this.nameDimension.top(Infinity).map(source => UniversalSearch.escape(source.name));
    }

    HistogramDataActions.load(this.state.range, filters, SCREEN_RESOLUTION)
      .then((histogramData) => {
        this.setState({
          resolution: histogramData.interval,
          reloadingHistogram: false,
          histogramDataAvailable: histogramData.histogram.length >= 2,
        });
        this._resetHistogram(histogramData.histogram);
      });

    this.setState({ reloadingHistogram: true });
  },

  _resetHistogram(histogram) {
    const lineChartFilters = this.refs.sourceLineChart.getFilters();
    this.valueDimension.filterAll();
    this.refs.sourceLineChart.clearFilters();
    this.histogramData.remove();
    this.histogramData.add(histogram);

    lineChartFilters.forEach(filter => this.refs.sourceLineChart.setFilter(filter));

    dc.redrawAll();
  },

  _resizeCallback() {
    this.eventThrottler.throttle(() => this._updateWidth());
  },

  // redirect old range format (as query parameter) to new format (deep link)
  _redirectToRange(range) {
    // if range is ill formatted, we take care of it in the deep link handling
    window.location.replace(`sources#${range}`);
  },

  _getRangeFromOldQueryFormat() {
    let query = window.location.search;
    if (query) {
      if (query.indexOf('?') === 0 && query.length > 1) {
        query = query.substr(1, query.length - 1);
        const range = Qs.parse(query).range;
        if (range) {
          return range;
        }
      }
    }
    return null;
  },

  _applyRangeFromHash() {
    const range = this._getRangeFromHash();
    this.changeRange(range, false);
  },

  applyRangeParameter() {
    const range = this._getRangeFromOldQueryFormat();
    if (range) {
      this._redirectToRange(range);
    } else {
      this._applyRangeFromHash();
    }
  },

  _getRangeFromHash() {
    const hash = window.location.hash;
    if (hash.indexOf('#') !== 0) {
      return DEFAULT_RANGE_IN_SECS;
    }
    const hashContent = hash.substring(1);
    if (hash.indexOf('&') < 0) {
        // If there is only one param in the hash, return it
      return hashContent;
    }
      // If there are more than one params in the hash, return the numeric one
    const match = hashContent.match(/(\d+)=&/);
    return (match && match.length > 0) ? match[1] : DEFAULT_RANGE_IN_SECS;
  },

  resetSourcesFilters() {
    this.refs.sourcePieChart.clearFilters();
    this.nameDimension.filterAll();
    this.loadHistogramData();
    this._toggleResetButtons();
    dc.redrawAll();
  },

  resetHistogramFilters() {
    this.valueDimension.filterAll();
    this.refs.sourceLineChart.clearFilters();
    dc.redrawAll();
  },

  _updateWidth() {
    SCREEN_RESOLUTION = $(window).width();
    this.refs.sourcePieChart.updateWidth();
    this.refs.sourceLineChart.updateWidth();
    dc.renderAll();
  },

  syncRangeWithQuery() {
    const rangeSelectBox = this.refs.rangeSelector;
    if (Number(rangeSelectBox.value) === 0) {
      SearchStore.changeTimeRange('relative', { relative: 0 });
    } else {
      const $selectedOptions = $(':selected', rangeSelectBox);
      const text = $selectedOptions && $selectedOptions[0] && $selectedOptions[0].text;
      SearchStore.changeTimeRange('keyword', { keyword: text });
    }
  },

  _toggleResetButtons() {
    // We only need to toggle the datatable reset button, dc will take care of the other reset buttons
    if (this.refs.sourcePieChart.getFilters().length !== 0) {
      $('#dc-sources-result-reset').show();
    } else {
      $('#dc-sources-result-reset').hide();
    }
  },

  changeRange(range, keepChangeInHistory) {
    let effectiveRange = range;

    if (effectiveRange !== undefined) {
      effectiveRange = Number(effectiveRange);
    }

    if (typeof effectiveRange === 'undefined' || SUPPORTED_RANGES_IN_SECS.indexOf(effectiveRange) === -1) {
      effectiveRange = DEFAULT_RANGE_IN_SECS;
    }

    if (this.state.range === effectiveRange) {
      return;
    }

    // when range is changed the filter in line chart (corresponding to the brush) does not make any sense any more
    this.valueDimension.filterAll();
    this.refs.sourceLineChart.clearFilters();
    this.syncRangeWithQuery();
    if (keepChangeInHistory) {
      window.location.hash = `#${effectiveRange}`;
    } else {
      window.location.replace(`#${effectiveRange}`);
    }
    this.setState({ range: effectiveRange, histogramDataAvailable: true, loading: true }, () => this.loadData());
  },

  _onRangeChanged(event) {
    const value = event.target.value;
    this.changeRange(value, true);
  },

  _filterSources() {
    this.filterDimension.filter((name) => {
      return name.indexOf(this.state.filter) !== -1;
    });
  },

  render() {
    const emptySources = (
      <div className="row content">
        <div className="col-md-12">
          <div className="alert alert-info">
            No message sources found for this time range. Did you try using a different one&#63;
          </div>
        </div>
      </div>
    );

    const resultsStyle = (this.state.renderResultTable ? null : { display: 'none' });
    const results = (
      <div style={resultsStyle}>
        <div className="row content">
          <SourceLineChart ref="sourceLineChart"
                           reloadingHistogram={this.state.reloadingHistogram}
                           histogramDataAvailable={this.state.histogramDataAvailable}
                           resolution={this.state.resolution}
                           resetFilters={this.resetHistogramFilters} />
        </div>
        {this.state.loading ?
          <div className="row content"><div style={{ marginLeft: 10 }}><Spinner /></div></div> :
          null}
        <div className="row content" style={{ display: this.state.loading ? 'none' : 'block' }}>
          <div className="col-md-7">
            <SourceDataTable ref="sourceDataTable" resetFilters={this.resetSourcesFilters}
                             setSearchFilter={this.setSearchFilter} numberOfTopValues={this.NUMBER_OF_TOP_VALUES} />
          </div>
          <div className="col-md-3 col-md-offset-1">
            <SourcePieChart ref="sourcePieChart" resetFilters={this.resetSourcesFilters}
                            numberOfTopValues={this.NUMBER_OF_TOP_VALUES} />
          </div>
        </div>
      </div>
    );

    return (
      <div>
        <div className="row content">
          <div className="col-md-12">
            <div>
              <div className="pull-right">
                <select ref="rangeSelector" className="sources-range form-control input-sm" value={this.state.range}
                        onChange={this._onRangeChanged}>
                  <option value={hoursToSeconds(1)}>Last Hour</option>
                  <option value={daysToSeconds(1)}>Last Day</option>
                  <option value={daysToSeconds(7)}>Last Week</option>
                  <option value={daysToSeconds(31)}>Last Month</option>
                  <option value={daysToSeconds(365)}>Last Year</option>
                  <option value="0">All</option>
                </select>
              </div>
              <h1>Sources</h1>
            </div>
            <p className="description">
              This is a list of all sources that sent in messages to Graylog. Note that the list is
              cached for a few seconds so you might have to wait a bit until a new source appears.
            </p>

            <SupportLink>
              {' Use your mouse to interact with the table and graphs on this page, and get a better ' +
              'overview of the sources sending data into Graylog.'}
            </SupportLink>
          </div>
        </div>

        {this.state.renderResultTable ? null : emptySources}
        {results}

      </div>
    );
  },
});

export default SourceOverview;
