/* jshint -W107 */

'use strict';

var React = require('react');

var $ = require('jquery');

var d3 = require('d3');
var dc = require('dc');
var numeral = require('numeral');

var UniversalSearch = require('../../logic/search/UniversalSearch');

var othersThreshold = 5;
var othersName = "Others";

var SourceDataTable = React.createClass({
    getInitialState() {
        return {
            numberOfSources: 100
        };
    },
    renderDataTable(dimension, onDataFiltered) {
        var dataTableDomNode = $("#dc-sources-result")[0];
        this._dataTable = dc.dataTable(dataTableDomNode);
        this._dataTable
            .dimension(dimension)
            .group((d) => d.percentage > othersThreshold ? "Top Sources" : othersName)
            .size(this.state.numberOfSources)
            .columns([
                (d) => "<button class='btn btn-mini btn-link dc-search-button' title='Search for this source'><i class='fa fa-search'></i></button>",
                (d) => "<a href='javascript:undefined' class='dc-filter-link' title='Filter this source'>" + d.name +"</a>",
                (d) => d.percentage.toFixed(2) + "%",
                (d) => numeral(d.message_count).format("0,0")
            ])
            .sortBy((d) => d.message_count)
            .order(d3.descending)
            .on('renderlet', (table) => {
                table.selectAll(".dc-table-group").classed("info", true);
                this._addSourceToSearchBarListener(table);
                this._filterSourceListener(table, onDataFiltered);
            });
    },
    _addSourceToSearchBarListener(table) {
        table.selectAll("td.dc-table-column._0 button.dc-search-button").on("click", () => {
            // d3 doesn't pass any data to the onclick event as the buttons do not
            // have any. Instead, we need to get it from the table element.
            var parentTdElement = $(d3.event.target).parents("td.dc-table-column._0");
            var datum = d3.selectAll(parentTdElement).datum();
            var source = datum.name;
            UniversalSearch.addSegment(UniversalSearch.createSourceQuery(source), UniversalSearch.orOperator());
            if (d3.event.altKey) {
                UniversalSearch.submit();
            }
        });
    },
    _filterSourceListener(table, onDataFiltered) {
        table.selectAll("td.dc-table-column._1 a.dc-filter-link").on("click", () => {
            var parentTdElement = $(d3.event.target).parents("td.dc-table-column._1");
            var datum = d3.selectAll(parentTdElement).datum();

            onDataFiltered(datum.name);
        });
    },
    redraw() {
        this._dataTable.redraw();
    },
    clearFilters() {
        this._dataTable.filterAll();
    },
    getFilters() {
        return this._dataTable ? this._dataTable.filters() : [];
    },
    setFilter(filter) {
        this._dataTable.filter(filter);
    },
    changeNumberOfSources(numberOfSources) {
        this._dataTable
            .size(numberOfSources)
            .redraw();
    },
    _onNumberOfSourcesChanged(event) {
        this.setState({numberOfSources: event.target.value}, () => {
            this.changeNumberOfSources(this.state.numberOfSources);
        });
    },
    _onFilterChanged(event) {
        this.props.setSearchFilter(event.target.value);
    },
    render() {
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

        return (
          <div>
              <h3><i className="fa fa-th-list"></i> Selected sources&nbsp;
                  <small><a href="javascript:undefined" id="dc-sources-result-reset" className="reset" onClick={this.props.resetFilters} title="Reset filter" style={{"display": "none"}}><i className="fa fa-remove"></i> Reset filter</a></small>
              </h3>
              <div className="row sources-filtering">
                  <div className="col-md-6">
                      <div className="form-horizontal pull-left">
                          <div className="input-group input-group-sm">
                              <input type="text" className="form-control" onChange={this._onFilterChanged} placeholder="Search"/>
                          </div>
                      </div>
                  </div>
                  <div className="col-md-6">
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
        );
    }
});

module.exports = SourceDataTable;