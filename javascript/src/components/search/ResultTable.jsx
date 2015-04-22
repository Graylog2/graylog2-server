'use strict';

var React = require('react');
var MessageTableEntry = require('./MessageTableEntry');
var MessageTablePaginator = require('./MessageTablePaginator');

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
            <h1 className="pull-left">Messages</h1>

            <MessageTablePaginator position="top"/>

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

            <MessageTablePaginator position="bottom"/>
        </div>);
    }
});

module.exports = ResultTable;