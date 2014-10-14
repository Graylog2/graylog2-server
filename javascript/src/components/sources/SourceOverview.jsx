/** @jsx React.DOM */

'use strict';

var React = require('react');

var $ = require('jquery');

var crossfilter = require('crossfilter');
var d3 = require('d3');
var dc = require('dc/dc');

var SourcesStore = require('../../stores/sources/SourcesStore');

var daysToSeconds = (days) => moment.duration(days, 'days').as('seconds');

var SourceOverview = React.createClass({
    getInitialState() {
        this.sourcesData = crossfilter();
        return {
            range: daysToSeconds(1),
            filter: '',
            renderResultTable: false
        };
    },
    renderPieChart() {

    },
    renderDataTable() {
        var nameDimension = this.sourcesData.dimension(function (d) {
            return d.name;
        });

        var dataTableDomNode = $("#dc-sources-result")[0];
        var dataTable = dc.dataTable(dataTableDomNode)
            .dimension(nameDimension)
            .group(function (d) {
                return "Top Sources";
            })
            .size(50)
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
                function (d) {
                    return d.messageCount;
                }
            ])
            .sortBy(function (d) {
                return d.messageCount;
            })
            .order(d3.descending)
            .renderlet(function (table) {
                table.selectAll(".dc-table-group").classed("info", true);
            });

        dc.renderAll();
    },
    componentDidMount() {
        SourcesStore.addChangeListener(this._onSourcesChanged);
        this.renderDataTable();
        this.renderPieChart();
        SourcesStore.loadSources(this.state.range);
    },
    componentWillUnmount() {
        SourcesStore.removeChangeListener(this._onSourcesChanged);
    },
    _onSourcesChanged() {
        // TODO: save filters once we have some
        this.sourcesData.remove();
        this.sourcesData.add(SourcesStore.getSources());
        dc.redrawAll();
        this.setState({renderResultTable: this.sourcesData.size() !== 0});
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
                    <th>Message count</th>
                </tr>
            </thead>
        </table>);

        return (
            <div>
                <div className="row-fluid">
                    <div>
                        <select className="sources-range pull-right" value={this.state.range}>
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
                {this.state.renderResultTable ? null : emptySources}
                {resultTable}
            </div>
        );
    }

});

module.exports = SourceOverview;


