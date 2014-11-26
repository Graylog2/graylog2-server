/* global activateTimerangeChooser */
/* jshint -W107 */

'use strict';

var React = require('react');

var $ = require('jquery');

var crossfilter = require('crossfilter');
var dc = require('dc');
var Qs = require('qs');
var Router = require('react-router');

var SourcesStore = require('../../stores/sources/SourcesStore');
var HistogramDataStore = require('../../stores/sources/HistogramDataStore');

var SourceDataTable = require('./SourceDataTable');
var SourcePieChart = require('./SourcePieChart');
var SourceLineChart = require('./SourceLineChart');

var UniversalSearch = require('../search/UniversalSearch');

var daysToSeconds = (days) => moment.duration(days, 'days').as('seconds');

var DEFAULT_RANGE_IN_SECS = daysToSeconds(1);
var SUPPORTED_RANGES_IN_SECS = [daysToSeconds(1), daysToSeconds(7), daysToSeconds(31), daysToSeconds(365), 0];

var SCREEN_RESOLUTION = $(window).width();

var resizeMutex;

var SourceOverview = React.createClass({
    mixins: [ Router.State ],
    getInitialState() {
        this.sourcesData = crossfilter();
        this.filterDimension = this.sourcesData.dimension((d) => d.name);
        this.nameDimension = this.sourcesData.dimension((d) => d.name);
        this.nameMessageGroup = this.nameDimension.group().reduceSum((d) => d.message_count);

        this.messageCountDimension = this.sourcesData.dimension((d) => d.message_count);

        this.histogramData = crossfilter();
        this.valueDimension = this.histogramData.dimension((d) => new Date(d.x * 1000));
        this.valueGroup = this.valueDimension.group().reduceSum((d) => d.y);

        return {
            range: null,
            resolution: 'minute',
            filter: '',
            renderResultTable: false,
            histogramDataAvailable: true,
            reloadingHistogram: false
        };
    },
    componentDidMount() {
        SourcesStore.addChangeListener(this._onSourcesChanged);
        HistogramDataStore.addChangeListener(this._onHistogramDataChanged);
        this.refs.sourceDataTable.renderDataTable(this.messageCountDimension, (sourceName) => {
            this.refs.sourcePieChart.setFilter(sourceName);
            this._toggleResetButtons();
            dc.redrawAll();
            this.loadHistogramData();
        });
        this.refs.sourcePieChart.renderPieChart(this.nameDimension, this.nameMessageGroup, () => {
            this.loadHistogramData();
            this._toggleResetButtons();
        });
        this.refs.sourceLineChart.renderLineChart(this.valueDimension, this.valueGroup, this.syncRangeWithQuery);
        this.applyRangeParameter();
        dc.renderAll();
        $(window).on('resize', this._resizeCallback);
        // register them live as we do not know if those buttons are currently in the DOM
        $(document).on("click", ".sidebar-hide", () => this._updateWidth());
        $(document).on("click", ".sidebar-show", () => this._updateWidth());
        UniversalSearch.init();
    },
    componentWillUnmount() {
        SourcesStore.removeChangeListener(this._onSourcesChanged);
        HistogramDataStore.removeChangeListener(this._onHistogramDataChanged);
        $(window).off("resize", this._resizeCallback);
        $(document).off("click", ".sidebar-hide", () => this._updateWidth());
        $(document).off("click", ".sidebar-show", () => this._updateWidth());
    },
    componentWillReceiveProps(newProps) {
        var range = newProps.params.range;
        this.changeRange(range);
    },
    loadHistogramData() {
        var filters;

        if (this.refs.sourcePieChart.getFilters().length !== 0 || this.refs.sourceDataTable.getFilters().length !== 0) {
            filters = this.nameDimension.top(Infinity).map((source) => UniversalSearch.escape(source.name));
        }
        HistogramDataStore.loadHistogramData(this.state.range, filters, SCREEN_RESOLUTION);
        this.setState({reloadingHistogram: true});
    },
    loadData() {
        SourcesStore.loadSources(this.state.range);
        this.loadHistogramData();
    },
    _resizeCallback() {
        // Call resizedWindow() only at end of resize event so we do not trigger all the time while resizing.
        clearTimeout(resizeMutex);
        resizeMutex = setTimeout(() => this._updateWidth(), 200);
    },
    applyRangeParameter() {
        var range;
        // redirect old range format (as query parameter) to new format (deep link)
        var query = window.location.search;
        if (query) {
            if (query.indexOf("?") === 0 && query.length > 1) {
                query = query.substr(1, query.length - 1);
                range = Qs.parse(query)["range"];
                if (range) {
                    // if range is ill formatted, we take care of it in the deep link handling
                    window.location.replace("sources#/" + range);
                    return;
                }
            }
        }
        range = this.getParams().range;
        this.changeRange(range);
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
    _resetSources(sources) {
        /*
         * http://stackoverflow.com/questions/23500546/replace-crossfilter-data-restore-dimensions-and-groups
         * It looks like dc interacts with crossfilter to represent the graphs and apply some filters
         * on the crossfilter dimension, but it also stores those filters internally. That means that
         * we need to remove the dimension and graphs filters, but we only need to reapply filters to the
         * graphs, dc will propagate that to the crossfilter dimension.
         */
        var pieChartFilters = this.refs.sourcePieChart.getFilters();
        var dataTableFilters = this.refs.sourceDataTable.getFilters();
        this.nameDimension.filterAll();
        this.filterDimension.filterAll();
        this.refs.sourcePieChart.clearFilters();
        this.refs.sourceDataTable.clearFilters();
        this.sourcesData.remove();
        this.sourcesData.add(sources);

        pieChartFilters.forEach((filter)  => this.refs.sourcePieChart.setFilter(filter));
        dataTableFilters.forEach((filter) => this.refs.sourceDataTable.setFilter(filter));
        this._filterSources();

        dc.redrawAll();
    },
    _resetHistogram(histogram) {
        var lineChartFilters = this.refs.sourceLineChart.getFilters();
        this.valueDimension.filterAll();
        this.refs.sourceLineChart.clearFilters();
        this.histogramData.remove();
        this.histogramData.add(histogram);

        lineChartFilters.forEach((filter)  => this.refs.sourceLineChart.setFilter(filter));

        dc.redrawAll();
    },
    _onSourcesChanged() {
        var sources = SourcesStore.getSources();
        this._resetSources(sources);
        this.setState({renderResultTable: this.sourcesData.size() !== 0});
        this._updateWidth();
    },
    _onHistogramDataChanged() {
        var histogramData = HistogramDataStore.getHistogramData();
        this.setState({resolution: histogramData.interval, reloadingHistogram: false, histogramDataAvailable: histogramData.values.length >= 2 });
        this._resetHistogram(histogramData.values);
    },
    syncRangeWithQuery() {
        var rangeSelectBox = this.refs.rangeSelector.getDOMNode();
        if (Number(rangeSelectBox.value) === 0) {
            activateTimerangeChooser("relative", $('.timerange-selector-container .dropdown-menu a[data-selector-name="relative"]'));
            $('#relative-timerange-selector').val(0);
        } else {
            var selectedOptions = rangeSelectBox.selectedOptions;
            var text = selectedOptions && selectedOptions[0] && selectedOptions[0].text;
            activateTimerangeChooser("keyword", $('.timerange-selector-container .dropdown-menu a[data-selector-name="keyword"]'));
            $('#universalsearch .timerange-selector.keyword > input').val(text);
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
    changeRange(range) {
        if (range !== undefined) {
            range = Number(range);
        }

        if (typeof range === 'undefined' || SUPPORTED_RANGES_IN_SECS.indexOf(range) === -1) {
            range = DEFAULT_RANGE_IN_SECS;
        }

        if (this.state.range === range) {
            return;
        }

        // when range is changed the filter in line chart (corresponding to the brush) does not make any sense any more
        this.valueDimension.filterAll();
        this.refs.sourceLineChart.clearFilters();
        this.syncRangeWithQuery();
        window.location.hash = "#/" + range;
        this.setState({range: range, histogramDataAvailable: true}, () => this.loadData());
    },
    _onRangeChanged(event) {
        var value = event.target.value;
        this.changeRange(value);
    },
    _filterSources() {
        this.filterDimension.filter((name) => {
            // TODO: search for starts with instead? glob style?
            //return name.indexOf(this.state.filter) === 0;
            return name.indexOf(this.state.filter) !== -1;
        });
    },
    setSearchFilter(filter) {
        this.setState({filter: filter}, () => {
            this._filterSources();
            this.refs.sourceDataTable.redraw();
            this.refs.sourcePieChart.redraw();
        });
    },
    render() {
        var emptySources = <div className="alert alert-info">
            No message sources found for this time range. Did you try using a different one?
        </div>;

        var resultsStyle = this.state.renderResultTable ? null : {display: 'none'};
        var results = (
            <div style={resultsStyle}>
                <div className="row-fluid">
                    <SourceLineChart ref="sourceLineChart"
                        reloadingHistogram={this.state.reloadingHistogram}
                        histogramDataAvailable={this.state.histogramDataAvailable}
                        resolution={this.state.resolution}
                        resetFilters={this.resetHistogramFilters}/>
                </div>
                <div className="row-fluid">
                    <div className="span9">
                        <SourceDataTable ref="sourceDataTable" resetFilters={this.resetSourcesFilters} setSearchFilter={this.setSearchFilter}/>
                    </div>
                    <div className="span3">
                        <SourcePieChart ref="sourcePieChart" resetFilters={this.resetSourcesFilters}/>
                    </div>
                </div>
            </div>
        );

        return (
            <div>
                <div className="row-fluid">
                    <div>
                        <select ref="rangeSelector" className="sources-range pull-right" value={this.state.range} onChange={this._onRangeChanged}>
                            <option value={daysToSeconds(1)}>Last Day</option>
                            <option value={daysToSeconds(7)}>Last Week</option>
                            <option value={daysToSeconds(31)}>Last Month</option>
                            <option value={daysToSeconds(365)}>Last Year</option>
                            <option value="0">All</option>
                        </select>
                        <h1><i className="icon icon-download-alt"></i> Sources</h1>
                    </div>
                    <p style={{"marginTop": "15px"}}>
                        This is a list of all sources that sent in messages to Graylog2. Use the table and charts to interact
                        with the different sources and get a better understanding of them.
                        &nbsp;Note that the list is cached for a few seconds so you might have to wait a bit until a new source
                        appears.
                    </p>
                </div>

                {this.state.renderResultTable ? null : emptySources}
                {results}

            </div>
        );
    }

});

module.exports = SourceOverview;
