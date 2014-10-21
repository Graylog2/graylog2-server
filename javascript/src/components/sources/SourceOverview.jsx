/** @jsx React.DOM */

/* global activateTimerangeChooser, momentHelper, htmlEscape */
// TODO: is there a better way of doing this?
/* jshint -W107 */

'use strict';

var React = require('react');

var $ = require('jquery');

var crossfilter = require('crossfilter');
var d3 = require('d3');
var dc = require('dc');

var SourcesStore = require('../../stores/sources/SourcesStore');
var HistogramDataStore = require('../../stores/sources/HistogramDataStore');

var daysToSeconds = (days) => moment.duration(days, 'days').as('seconds');

var othersThreshold = 5;
var othersName = "Others";

var SourceOverview = React.createClass({
    getInitialState() {
        this.sourcesData = crossfilter();
        this.filterDimension = this.sourcesData.dimension((d) => d.name);
        this.nameDimension = this.sourcesData.dimension((d) => d.name);
        this.nameMessageGroup = this.nameDimension.group().reduceSum((d) => d.messageCount);
        this.othersDimension = this.sourcesData.dimension((d) => d.percentage > othersThreshold ? d.name : othersName);
        this.othersMessageGroup = this.othersDimension.group().reduceSum((d) => d.messageCount);

        this.histogramData = crossfilter();
        this.valueDimension = this.histogramData.dimension((d) => new Date(d.x * 1000));
        this.valueGroup = this.valueDimension.group().reduceSum((d) => d.y);

        this.querySources = [];

        return {
            range: daysToSeconds(1),
            resolution: 'minute',
            filter: '',
            renderResultTable: false,
            numberOfSources: 100
        };
    },
    loadHistogramData() {
        var filters = this.othersDimension.top(Infinity).map((source) => source.name);
        HistogramDataStore.loadHistogramData(this.state.range, filters);
    },
    loadData() {
        SourcesStore.loadSources(this.state.range);
        this.loadHistogramData();
    },
    componentDidMount() {
        SourcesStore.addChangeListener(this._onSourcesChanged);
        HistogramDataStore.addChangeListener(this._onHistogramDataChanged);
        this.renderDataTable();
        this.renderPieChart();
        this.renderLineChart();
        dc.renderAll();
        this.loadData();
    },
    componentWillUnmount() {
        SourcesStore.removeChangeListener(this._onSourcesChanged);
        HistogramDataStore.removeChangeListener(this._onHistogramDataChanged);
    },
    updatePieChartDimension() {
        // TODO: Decide if the pie chart should ever display grouped others (makes things complicated to program and to use, so disabled for now)
        //var onlyMinorValues = this.filterDimension.top(Infinity).reduce((reducedValue, current) => reducedValue && current.percentage < othersThreshold, true);
        var onlyMinorValues = true;
        if (onlyMinorValues) {
            this.pieChart
                .dimension(this.nameDimension)
                .group(this.nameMessageGroup);
        } else {
            this.pieChart
                .dimension(this.othersDimension)
                .group(this.othersMessageGroup);
        }
    },
    renderPieChart() {
        var pieChartDomNode = $("#dc-sources-pie-chart")[0];
        var pieChartWidth = $(pieChartDomNode).width();
        this.pieChart = dc.pieChart(pieChartDomNode);
        this.pieChart.width(pieChartWidth)
            .height(pieChartWidth)
            .innerRadius(pieChartWidth / 5)
            .dimension(this.othersDimension)
            .group(this.othersMessageGroup)
            .renderlet((chart) => {
                chart.selectAll("#dc-sources-pie-chart .pie-slice").on("click", (d, index) => {
                    this.loadHistogramData();
                });
            });
    },
    renderLineChart() {
        var lineChartDomNode = $("#dc-sources-line-chart")[0];
        $(lineChartDomNode).on('mouseup', (event) => {
            $(".timerange-selector-container").effect("bounce", {
                complete: function () {
                    // Submit search directly if alt key is pressed.
                    if (event.altKey) {
                        $("#universalsearch form").submit();
                    }
                }
            });
        });
        this.lineChart = dc.lineChart(lineChartDomNode);
        this.lineChart
            .width($(lineChartDomNode).width())
            .height(200)
            .margins({left: 50, right: 20, top: 20, bottom: 20})
            //.renderArea(true)
            .dimension(this.valueDimension)
            .group(this.valueGroup)
            .x(d3.time.scale())
            .xUnits(d3.time.minutes)
            .renderHorizontalGridLines(true)
            // FIXME: causes those nasty exceptions when rendering data (one per x axis tick)
            .elasticX(true)
            .elasticY(true)
            .on("filtered", (chart) => {
                dc.events.trigger(() => {

                    var filter = chart.filter();
                    if (filter) {
                        var fromDateTime = moment(filter[0]);
                        var toDateTime = moment(filter[1]);

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
    },
    renderDataTable() {
        var dataTableDomNode = $("#dc-sources-result")[0];
        this.dataTable = dc.dataTable(dataTableDomNode);
        this.dataTable
            .dimension(this.othersDimension)
            .group((d) => d.percentage > othersThreshold ? "Top Sources" : othersName)
            .size(this.state.numberOfSources)
            .columns([
                (d) => "<a href='javascript:undefined' class='dc-search-link'><i class='icon icon-search' title='Search for this source'></i></button>",
                (d) => "<a href='javascript:undefined' class='dc-filter-link' title='Filter this source'>" + d.name +"</a>",
                (d) => d.percentage.toFixed(2) + "%",
                (d) => d.messageCount
            ])
            .sortBy((d) => d.messageCount)
            .order(d3.descending)
            .renderlet((chart) => {
                chart.selectAll("#dc-sources-result tbody td.dc-table-column._0 a.dc-search-link").on("click", (_) => {
                    // d3 doesn't pass any data to the onclick event as the buttons do not
                    // have any. Instead, we need to get it from the table element.
                    var parentTdElement = $(d3.event.target).parents("td.dc-table-column._0");
                    var datum = d3.selectAll(parentTdElement).datum();

                    // toggles source
                    var index = this.querySources.indexOf(datum.name);
                    if (index === -1) {
                        this.querySources.push(datum.name);
                    } else {
                        this.querySources.splice(index, 1);
                    }

                    var queryString = this.querySources.map((source) => "source:"+htmlEscape(source)).join(" OR ");
                    var query = $("#universalsearch-query");
                    query.val(queryString);
                    query.effect("bounce");

                    // TODO: highlight query icon
                });
            })
            .renderlet((chart) => {
                chart.selectAll("#dc-sources-result tbody td.dc-table-column._1 a.dc-filter-link").on("click", (_) => {
                    var parentTdElement = $(d3.event.target).parents("td.dc-table-column._1");
                    var datum = d3.selectAll(parentTdElement).datum();

                    this.pieChart.filter(datum.name);
                    dc.redrawAll();
                    this.loadHistogramData();
                });
            })
            .renderlet((table) => table.selectAll(".dc-table-group").classed("info", true));
    },
    resetSourcesFilters() {
        this.pieChart.filterAll();
        this.othersDimension.filterAll();
        this.loadHistogramData();
        dc.redrawAll();
    },
    resetHistogramFilters() {
        this.valueDimension.filterAll();
        this.lineChart.filterAll();
        dc.redrawAll();
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
        this.othersDimension.filterAll();
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
    },
    _onHistogramDataChanged() {
        var histogramData = HistogramDataStore.getHistogramData();
        this.setState({resolution: histogramData.interval});
        this._resetHistogram(histogramData.values);
    },
    _syncRangeWithQuery: function () {
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
    _onRangeChanged(event) {
        // when range is changed the filter in line chart (corresponding to the brush) does not make any sense any more
        this.valueDimension.filterAll();
        this.lineChart.filterAll();
        this._syncRangeWithQuery();
        this.setState({range: event.target.value}, () => this.loadData());
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
        this.updatePieChartDimension();
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
        No message sources found. Looks like you did not send in any messages yet.
        </div>;

        var resultTableStyle = this.state.renderResultTable ? null : {display: 'none'};
        var resultTable = (<table id="dc-sources-result" className="sources table table-striped table-hover table-condensed" style={resultTableStyle}>
            <thead>
                <tr>
                    <th style={{width: "10px"}}></th>
                    <th>Source name</th>
                    <th>Percentage</th>
                    <th>Message count</th>
                </tr>
            </thead>
        </table>);

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
                    <div style={{"margin-top": "15px"}}>
                    This is a list of all sources that sent in messages to Graylog2. Use it to quickly search for all
                    messages of a specific source or get an overview of what systems are sending in how many messages.
                    &nbsp;
                        <strong>
                        Click on source name to prepare a query for it. Hold the Alt key while clicking to search right
                        away.
                        </strong>

                    &nbsp;Note that the list is cached for a few seconds so you might have to wait a bit until a new source
                    appears.
                    </div>
                </div>
                <div className="row-fluid">
                    <div id="dc-sources-line-chart" className="span12">
                        <h3><i className="icon icon-calendar"></i> Messages per {this.state.resolution} <small><a href="javascript:undefined" className="reset" onClick={this.resetHistogramFilters} style={{"display": "none"}}><i className="icon icon-repeat"></i></a></small></h3>
                    </div>
                </div>
                {this.state.renderResultTable ? null : emptySources}
                <div className="row-fluid">
                    <div className="span9">
                        <h3><i className="icon icon-th-list"></i> All sources selected</h3>
                        <div className="row-fluid sources-filtering">
                            <div className="span6">
                                <div className="form-horizontal pull-left">
                                    <div className="control-group">
                                        <label className="control-label">Search:</label>
                                        <div className="controls">
                                            <input type="search" className="input-medium" onChange={this._onFilterChanged}/>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div className="span6">
                                <div className="form-horizontal pull-right">
                                    <div className="control-group">
                                        <label className="control-label">Sources:</label>
                                        <div className="controls">
                                            <select className="input-small" onChange={this._onNumberOfSourcesChanged} value={this.state.numberOfSources}>
                                                <option value="1">1</option>
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
                            <h3><i className="icon icon-bar-chart"></i> Messages per source <small><a href="javascript:undefined" className="reset" onClick={this.resetSourcesFilters} style={{"display": "none"}}><i className="icon icon-repeat"></i></a></small></h3>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

});

module.exports = SourceOverview;