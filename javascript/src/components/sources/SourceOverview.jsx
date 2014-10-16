/** @jsx React.DOM */

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

var SourceOverview = React.createClass({
    getInitialState() {
        this.sourcesData = crossfilter();
        this.othersDimension = this.sourcesData.dimension((d) => d.percentage > othersThreshold ? d.name : 'Others');
        this.othersMessageGroup = this.othersDimension.group().reduceSum((d) => d.messageCount);

        this.histogramData = crossfilter();
        this.valueDimension = this.histogramData.dimension((d) => new Date(d.x * 1000));
        this.valueGroup = this.valueDimension.group().reduceSum((d) => d.y);

        return {
            range: daysToSeconds(1),
            filter: '',
            renderResultTable: false
        };
    },
    loadData() {
        SourcesStore.loadSources(this.state.range);
        HistogramDataStore.loadHistogramData(this.state.range);
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
    renderPieChart() {
        var pieChartDomNode = $("#dc-sources-pie-chart")[0];
        var pieChartWidth = $(pieChartDomNode).width();
        var pieChart = dc.pieChart(pieChartDomNode);
        pieChart.width(pieChartWidth)
            .height(pieChartWidth)
            .innerRadius(pieChartWidth / 5)
            .dimension(this.othersDimension)
            .group(this.othersMessageGroup)
            .renderlet((chart) => {
                chart.selectAll("#dc-sources-pie-chart .pie-slice").on("click", (d, index) => {
                    var filters = this.othersDimension.top(Infinity).map((source) => source.name);
                    HistogramDataStore.loadHistogramData(this.state.range, filters);
                });
            });
    },
    renderLineChart() {
        var lineChartDomNode = $("#dc-sources-line-chart")[0];
        dc.lineChart(lineChartDomNode)
            .width($(lineChartDomNode).width())
            .height(200)
            .margins({left: 50, right: 20, top: 20, bottom: 20})
            //.renderArea(true)
            .dimension(this.valueDimension)
            .group(this.valueGroup)
            .x(d3.time.scale())
            .xUnits(d3.time.minutes)
            .renderHorizontalGridLines(true)
            .elasticX(true)
            .elasticY(true);
    },
    renderDataTable() {
        var dataTableDomNode = $("#dc-sources-result")[0];
        dc.dataTable(dataTableDomNode)
            .dimension(this.othersDimension)
            .group((d) => d.percentage > othersThreshold ? "Top Sources" : "Others")
            .size(500)
            .columns([
                function (d) {
                    // TODO
                    /*
                     <a href="#" class="search-link" data-field="source" data-search-link-operator="OR" data-value="@source.getName">
                     @source.getName
                     </a>

                     */

                    return d.name;
                },
                (d) => d.percentage.toFixed(2) + "%",
                (d) => d.messageCount
            ])
            .sortBy((d) => d.messageCount)
            .order(d3.descending)
            .renderlet((table) => table.selectAll(".dc-table-group").classed("info", true));
    },
     _onSourcesChanged() {
        // TODO: save filters once we have some
        var sources = SourcesStore.getSources();
        this.sourcesData.remove();
        this.sourcesData.add(sources);
        dc.redrawAll();
        this.setState({renderResultTable: this.sourcesData.size() !== 0});
    },
    _onHistogramDataChanged() {
        // TODO: save filters once we have some
        var histogramData = HistogramDataStore.getHistogramData();
        // TODO do something with the rest of the data
        this.histogramData.remove();
        this.histogramData.add(histogramData.values);
        dc.redrawAll();
    },
    _onRangeChanged(event){
        this.setState({range: event.target.value}, () => this.loadData());
    },
    render() {
        var emptySources = <div className="alert alert-info">
        No message sources found. Looks like you did not send in any messages yet.
        </div>;

        var resultTableStyle = this.state.renderResultTable ? null : {display: 'none'};
        var resultTable = (<table id="dc-sources-result" className="sources table table-striped table-hover table-condensed" style={resultTableStyle}>
            <thead>
                <tr>
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
                        <select className="sources-range pull-right" value={this.state.range} onChange={this._onRangeChanged}>
                            <option value={daysToSeconds(1)}>Last Day</option>
                            <option value={daysToSeconds(7)}>Last Week</option>
                            <option value={daysToSeconds(31)}>Last Month</option>
                            <option value={daysToSeconds(365)}>Last Year</option>
                            <option value="0">All</option>
                        </select>
                        <h1>
                            <i className="icon icon-download-alt"></i>
                        Sources</h1>
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
                    </div>
                </div>
                {this.state.renderResultTable ? null : emptySources}
                <div className="row-fluid">
                    <div className="span9">
                    {resultTable}
                    </div>
                    <div id="dc-sources-pie-chart" className="span3">
                    </div>
                </div>
            </div>
        );
    }

});

module.exports = SourceOverview;


