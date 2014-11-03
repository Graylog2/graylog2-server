/** @jsx React.DOM */

/* global activateTimerangeChooser, momentHelper, graphHelper */
/* jshint -W107 */

'use strict';

var React = require('react');

var $ = require('jquery');

var crossfilter = require('crossfilter');
var d3 = require('d3');
var dc = require('dc');
var Qs = require('qs');

var SourcesStore = require('../../stores/sources/SourcesStore');
var HistogramDataStore = require('../../stores/sources/HistogramDataStore');

var UniversalSearch = require('../search/UniversalSearch');

var daysToSeconds = (days) => moment.duration(days, 'days').as('seconds');

var othersThreshold = 5;
var othersName = "Others";
var DEFAULT_RANGE_IN_SECS = daysToSeconds(1);
var SUPPORTED_RANGES_IN_SECS = [daysToSeconds(1), daysToSeconds(7), daysToSeconds(31), daysToSeconds(365), 0];

var SCREEN_RESOLUTION = $(window).width();

var resizeMutex;

var SourceOverview = React.createClass({
    getInitialState() {
        this.sourcesData = crossfilter();
        this.filterDimension = this.sourcesData.dimension((d) => d.name);
        this.nameDimension = this.sourcesData.dimension((d) => d.name);
        this.nameMessageGroup = this.nameDimension.group().reduceSum((d) => d.messageCount);

        this.histogramData = crossfilter();
        this.valueDimension = this.histogramData.dimension((d) => new Date(d.x * 1000));
        this.valueGroup = this.valueDimension.group().reduceSum((d) => d.y);

        return {
            range: null,
            resolution: 'minute',
            filter: '',
            renderResultTable: false,
            histogramDataAvailable: true,
            numberOfSources: 100,
            reloadingHistogram: false,
            lineChartWidth: "100%"
        };
    },
    loadHistogramData() {
        var filters;

        if (this.pieChart && (this.pieChart.filters().length !== 0 || this.dataTable.filters().length !== 0)) {
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
        this.applyRangeParameter();
        SourcesStore.addChangeListener(this._onSourcesChanged);
        HistogramDataStore.addChangeListener(this._onHistogramDataChanged);
        this.renderDataTable();
        this.renderPieChart();
        this.renderLineChart();
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
        range = this.props.params.range;
        this.changeRange(range);
    },
    renderPieChart() {
        var pieChartDomNode = $("#dc-sources-pie-chart")[0];
        var pieChartWidth = $(pieChartDomNode).width();
        this.pieChart = dc.pieChart(pieChartDomNode);
        this.pieChart
            .renderLabel(false)
            .dimension(this.nameDimension)
            .group(this.nameMessageGroup)
            .renderlet((chart) => {
                chart.selectAll(".pie-slice").on("click", () => {
                    this.loadHistogramData();
                    this._toggleResetButtons();
                });
            });
        this.configurePieChartWidth(pieChartWidth);
    },
    renderLineChart() {
        var lineChartDomNode = $("#dc-sources-line-chart")[0];
        var width = $(lineChartDomNode).width();
        $(lineChartDomNode).on('mouseup', (event) => {
            $(".timerange-selector-container").effect("bounce", {
                complete: function () {
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
    renderDataTable() {
        var dataTableDomNode = $("#dc-sources-result")[0];
        this.dataTable = dc.dataTable(dataTableDomNode);
        this.dataTable
            .dimension(this.sourcesData.dimension((d) => d.messageCount))
            .group((d) => d.percentage > othersThreshold ? "Top Sources" : othersName)
            .size(this.state.numberOfSources)
            .columns([
                (d) => "<button class='btn btn-mini btn-link dc-search-button' title='Search for this source'><i class='icon icon-search'></i></button>",
                (d) => "<a href='javascript:undefined' class='dc-filter-link' title='Filter this source'>" + d.name +"</a>",
                (d) => d.percentage.toFixed(2) + "%",
                (d) => d.messageCount
            ])
            .sortBy((d) => d.messageCount)
            .order(d3.descending)
            .renderlet((table) => {
                table.selectAll("td.dc-table-column._0 button.dc-search-button").on("click", () => {
                    // d3 doesn't pass any data to the onclick event as the buttons do not
                    // have any. Instead, we need to get it from the table element.
                    var parentTdElement = $(d3.event.target).parents("td.dc-table-column._0");
                    var datum = d3.selectAll(parentTdElement).datum();
                    var source = datum.name;
                    UniversalSearch.addSegment(UniversalSearch.createSourceQuery(source), UniversalSearch.orOperator());
                    if (window.event.altKey) {
                        UniversalSearch.submit();
                    }
                });
            })
            .renderlet((table) => {
                table.selectAll("td.dc-table-column._1 a.dc-filter-link").on("click", () => {
                    var parentTdElement = $(d3.event.target).parents("td.dc-table-column._1");
                    var datum = d3.selectAll(parentTdElement).datum();

                    this.pieChart.filter(datum.name);
                    this._toggleResetButtons();
                    dc.redrawAll();
                    this.loadHistogramData();
                });
            })
            .renderlet((table) => table.selectAll(".dc-table-group").classed("info", true));
    },
    resetSourcesFilters() {
        this.pieChart.filterAll();
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
    configurePieChartWidth: function (pieChartWidth) {
        this.pieChart.width(pieChartWidth)
            .height(pieChartWidth)
            .radius(pieChartWidth / 2 - 10)
            .innerRadius(pieChartWidth / 5);
    },
    configureLineChartWidth: function (lineChartWidth) {
        this.lineChart
            .width(lineChartWidth);
        this.setState({lineChartWidth: String(lineChartWidth) + "px"});
    },
    _updateWidth() {
        SCREEN_RESOLUTION = $(window).width();

        var pieChartDomNode = $("#dc-sources-pie-chart").parent();
        var pieChartWidth = pieChartDomNode.width();
        this.configurePieChartWidth(pieChartWidth);

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
        var pieChartFilters = this.pieChart.filters();
        var dataTableFilters = this.dataTable.filters();
        this.nameDimension.filterAll();
        this.filterDimension.filterAll();
        this.pieChart.filterAll();
        this.dataTable.filterAll();
        this.sourcesData.remove();
        this.sourcesData.add(sources);

        pieChartFilters.forEach((filter)  => this.pieChart.filter(filter));
        dataTableFilters.forEach((filter) => this.dataTable.filter(filter));
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
        if (this.pieChart.filter()) {
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
    _onNumberOfSourcesChanged(event) {
        this.setState({numberOfSources: event.target.value}, () => {
            this.dataTable
                .size(this.state.numberOfSources)
                .redraw();
        });
    },
    _filterSources() {
        this.filterDimension.filter((name) => {
            // TODO: search for starts with instead? glob style?
            //return name.indexOf(this.state.filter) === 0;
            return name.indexOf(this.state.filter) !== -1;
        });
    },
    _onFilterChanged(event) {
        this.setState({filter: event.target.value}, () => {
            this._filterSources();
            this.dataTable.redraw();
            this.pieChart.redraw();
        });
    },
    render() {
        var emptySources = <div className="alert alert-info">
            No message sources found for this time range. Did you try using a different one?
        </div>;

        var resultTable = (<table id="dc-sources-result" className="sources table table-striped table-hover table-condensed">
            <thead>
                <tr>
                    <th style={{width: "10px"}}></th>
                    <th>Source name</th>
                    <th>Percentage</th>
                    <th>Message count</th>
                </tr>
            </thead>
        </table>);

        var loadingSpinnerStyle = {display: this.state.reloadingHistogram ? 'block' : 'none', width: this.state.lineChartWidth};
        var loadingSpinner = (
            <div className="sources overlay" style={loadingSpinnerStyle}>
                <i className="icon-spin icon-refresh spinner"></i>
            </div>
        );

        var noDataOverlayStyle = {display: this.state.histogramDataAvailable ? 'none' : 'block', width: this.state.lineChartWidth};
        var noDataOverlay = (
            <div className="sources overlay" style={noDataOverlayStyle}>No data available</div>
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
                        <h3><i className="icon icon-th-list"></i> Selected sources&nbsp;
                            <small><a href="javascript:undefined" id="dc-sources-result-reset" className="reset" onClick={this.resetSourcesFilters} title="Reset filter" style={{"display": "none"}}><i className="icon icon-retweet"></i></a></small>
                        </h3>
                        <div className="row-fluid sources-filtering">
                            <div className="span6">
                                <div className="form-horizontal pull-left">
                                    <div className="control-group">
                                        <input type="search" className="input-medium" onChange={this._onFilterChanged} placeholder="Search"/>
                                    </div>
                                </div>
                            </div>
                            <div className="span6">
                                <div className="form-horizontal pull-right">
                                    <div className="control-group">
                                        <label className="control-label">Results:</label>
                                        <div className="controls">
                                            <select className="input-small" onChange={this._onNumberOfSourcesChanged} value={this.state.numberOfSources}>
                                                <option value="10">10</option>
                                                <option value="50">50</option>
                                                <option value="100">100</option>
                                                <option value="500">500</option>
                                            </select>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        {resultTable}
                    </div>
                    <div className="span3">
                        <div id="dc-sources-pie-chart">
                            <h3><i className="icon icon-bar-chart"></i> Messages per source&nbsp;
                                <small><a href="javascript:undefined" className="reset" onClick={this.resetSourcesFilters} title="Reset filter" style={{"display": "none"}}><i className="icon icon-retweet"></i></a></small>
                            </h3>
                        </div>
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
                    <p style={{"margin-top": "15px"}}>
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
