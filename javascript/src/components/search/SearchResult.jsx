'use strict';

var React = require('react');
var MessageTableEntry = require('./MessageTableEntry');
var SearchSidebar = require('./SearchSidebar');
var PageItem = require('react-bootstrap').PageItem;

var SearchResult = React.createClass({
    getInitialState() {
        return {
            page: 1,
            expandedMessages: {}
        };
    },

    _toggleMessageDetail(id) {
        this.state.expandedMessages[id] = !this.state.expandedMessages[id];
        this.setState(this.state);
    },

    render() {

        return (
            <div >
                <div className="col-md-3" id="sidebar">
                    <SearchSidebar result={this.props.result} />
                </div>
                <div className="col-md-9" id="main-content-sidebar">
                    <div className="content-col">
                        <ul className="pagination">
                            <PageItem href="#">Previous</PageItem>
                            <PageItem href="#">1</PageItem>
                            <PageItem href="#">Next</PageItem>
                        </ul>

                        <h1>Messages</h1>

                        <table className="table table-condensed messages">
                            <thead>
                            <tr>
                                <th style={{style: 180}}>Timestamp</th>
                                <th style={{style: 180}} id="result-th-36cd38f49b9afa08222c0dc9ebfe35eb">Source</th>
                            </tr>
                            </thead>
                            { this.props.result.messages.map((message) => <MessageTableEntry key={message.id}
                                                                                             message={message}
                                                                                             expanded={this.state.expandedMessages[message.id]}
                                                                                             toggleDetail={this._toggleMessageDetail}/>) }
                        </table>

                        <p>some search result for query {this.props.result['original_query']}</p>
                    </div>
                </div>
            </div>);
    }
});

module.exports = SearchResult;