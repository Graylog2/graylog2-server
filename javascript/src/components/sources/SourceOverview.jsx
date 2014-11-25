/* global activateTimerangeChooser, momentHelper, graphHelper */
/* jshint -W107 */

'use strict';

var React = require('react');

var $ = require('jquery');

var crossfilter = require('crossfilter');
var d3 = require('d3');
var dc = require('dc');
var Qs = require('qs');
var Router = require('react-router');

var SourcesStore = require('../../stores/sources/SourcesStore');
var HistogramDataStore = require('../../stores/sources/HistogramDataStore');

var SourceDataTable = require('./SourceDataTable');
var SourcePieChart = require('./SourcePieChart');

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
            reloadingHistogram: false,
            lineChartWidth: "100%"
        };
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
        this.renderLineChart();
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
    renderLineChart() {
        var lineChartDomNode = $("#dc-sources-line-chart")[0];
        var width = $(lineChartDomNode).width();
        $(lineChartDomNode).on('mouseup', (event) => {
            $(".timerange-selector-container").effect("bounce", {
                complete: () => {
                    // Submit search directly if alt key is pressed.
                    if (event.altKey) {
                        UniversalSearch.submit();
                    }
                }
            });
        });
        this.lineChart = dc.lineChart(lineChartDomNode);
        this.lineChart
            .height(200)
            .margins({left: 35, right: 20, top: 20, bottom: 20})
            .dimension(this.valueDimension)
            .group(this.valueGroup)
            .x(d3.time.scale())
            .renderHorizontalGridLines(true)
            // FIXME: causes those nasty exceptions when rendering data (one per x axis tick)
            .elasticX(true)
            .elasticY(true)
            .on("filtered", (chart) => {
                dc.events.trigger(() => {
                    var filter = chart.filter();
                    if (filter) {
                        var fromDateTime = momentHelper.toUserTimeZone(filter[0]);
                        var toDateTime = momentHelper.toUserTimeZone(filter[1]);

                        activateTimerangeChooser("absolute", $('.timerange-selector-container .dropdown-menu a[data-selector-name="absolute"]'));
                        var fromInput = $('#universalsearch .absolute .absolute-from-human');
                        var toInput = $('#universalsearch .absolute .absolute-to-human');

                        fromInput.val(fromDateTime.format(momentHelper.DATE_FORMAT_TZ));
                        toInput.val(toDateTime.format(momentHelper.DATE_FORMAT_TZ));
                    } else {
                        this._syncRangeWithQuery();
                    }
                });
            });
        this.configureLineChartWidth(width);
        this.lineChart.xAxis()
            .ticks(graphHelper.customTickInterval())
            .tickFormat(graphHelper.customDateTimeFormat());
        this.lineChart.yAxis()
            .ticks(6)
            .tickFormat(d3.format("s"));
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
        this.lineChart.filterAll();
        dc.redrawAll();
    },
    configureLineChartWidth(lineChartWidth) {
        this.lineChart
            .width(lineChartWidth);
        this.setState({lineChartWidth: String(lineChartWidth) + "px"});
    },
    _updateWidth() {
        SCREEN_RESOLUTION = $(window).width();

        var pieChartDomNode = $("#dc-sources-pie-chart").parent();
        var pieChartWidth = pieChartDomNode.width();
        this.refs.sourcePieChart.configurePieChartWidth(pieChartWidth);

        var lineChartDomNode = $("#dc-sources-line-chart");
        var lineChartWidth = lineChartDomNode.width();
        this.configureLineChartWidth(lineChartWidth);

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
        var lineChartFilters = this.lineChart.filters();
        this.valueDimension.filterAll();
        this.lineChart.filterAll();
        this.histogramData.remove();
        this.histogramData.add(histogram);

        lineChartFilters.forEach((filter)  => this.lineChart.filter(filter));

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
    _syncRangeWithQuery() {
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
        if (this.lineChart) {
            this.lineChart.filterAll();
        }
        this._syncRangeWithQuery();
        // TODO: is this the best way of updating the URL???
        //window.location.href = "sources#/" + range;
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

        var loadingSpinnerStyle = {display: this.state.reloadingHistogram ? 'block' : 'none', width: this.state.lineChartWidth};
        var loadingSpinner = (
            <div className="sources overlay" style={loadingSpinnerStyle}>
                <i className="icon-spin icon-refresh spinner"></i>
            </div>
        );

        var noDataOverlayStyle = {display: this.state.histogramDataAvailable ? 'none' : 'block', width: this.state.lineChartWidth};
        var noDataOverlay = (
            <div className="sources overlay" style={noDataOverlayStyle}>Not enough data</div>
        );

        var resultsStyle = this.state.renderResultTable ? null : {display: 'none'};
        var results = (
            <div style={resultsStyle}>
                <div className="row-fluid">
                    <div id="dc-sources-line-chart" className="span12">
                        <h3>
                            <i className="icon icon-calendar"></i> Messages per {this.state.resolution}&nbsp;
                            <small>
                                <a href="javascript:undefined" className="reset" onClick={this.resetHistogramFilters} title="Reset filter" style={{"display": "none"}}>
                                    <i className="icon icon-retweet"></i>
                                </a>
                            </small>
                        </h3>
                        {loadingSpinner}
                        {noDataOverlay}
                    </div>
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
