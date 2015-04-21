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

    _fieldColumns() {
        return this.props.selectedFields.delete('message');
    },
    _columnStyle(fieldName) {
        if (fieldName.toLowerCase() === 'source') {
            return {width: 180};
        }
        return {};
    },

    render() {
        var selectedColumns = this._fieldColumns();
        return (<div className="content-col">
            <ul className="pagination">
                <PageItem href="#">Previous</PageItem>
                <PageItem href="#">1</PageItem>
                <PageItem href="#">Next</PageItem>
            </ul>

            <h1>Messages</h1>

            <div className="table-responsive">
                <table className="table table-condensed messages">
                    <thead>
                    <tr>
                        <th style={{width: 180}}>Timestamp</th>
                        { selectedColumns.map(selectedFieldName => <th key={selectedFieldName}
                                                                       style={this._columnStyle(selectedFieldName)}>{selectedFieldName}</th>) }
                    </tr>
                    </thead>
                    { this.props.messages.map((message) => <MessageTableEntry key={message.id}
                                                                              message={message}
                                                                              showMessageRow={this.props.selectedFields.contains('message')}
                                                                              selectedFields={selectedColumns}
                                                                              expanded={this.state.expandedMessages[message.id]}
                                                                              toggleDetail={this._toggleMessageDetail}/>) }
                </table>
            </div>

        </div>);
    }
});

module.exports = ResultTable;