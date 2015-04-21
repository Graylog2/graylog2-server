'use strict';

var React = require('react');
var SearchSidebar = require('./SearchSidebar');
var ResultTable = require('./ResultTable');
var LegacyHistogram= require('./LegacyHistogram');

var SearchResult = React.createClass({
    render() {
        return (
            <div >
                <div className="col-md-3" id="sidebar">
                    <SearchSidebar result={this.props.result}/>
                </div>
                <div className="col-md-9" id="main-content-sidebar">
                    <LegacyHistogram formattedHistogram={this.props.formattedHistogram} histogram={this.props.histogram} />

                    <ResultTable messages={this.props.result.messages} page={this.props.currentPage} />

                </div>
            </div>);
    }
});

module.exports = SearchResult;