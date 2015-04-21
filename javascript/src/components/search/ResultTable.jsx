'use strict';

var React = require('react');
var MessageTableEntry = require('./MessageTableEntry');
var PageItem = require('react-bootstrap').PageItem;

var ResultTable = React.createClass({
    getInitialState() {
        return {
            expandedMessages: {}
        };
    },

    _toggleMessageDetail(id) {
        this.state.expandedMessages[id] = !this.state.expandedMessages[id];
        this.setState(this.state);
    },

    render() {
        return (<div className="content-col">
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
                { this.props.messages.map((message) => <MessageTableEntry key={message.id}
                                                                          message={message}
                                                                          expanded={this.state.expandedMessages[message.id]}
                                                                          toggleDetail={this._toggleMessageDetail}/>) }
            </table>

        </div>);
    }
});

module.exports = ResultTable;