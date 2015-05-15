'use strict';

var React = require('react');
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Button = require('react-bootstrap').Button;
var MessageTableEntry = require('./MessageTableEntry');
var MessageTablePaginator = require('./MessageTablePaginator');
var Immutable = require('immutable');

var StreamsStore = require('../../stores/streams/StreamsStore');

var ResultTable = React.createClass({
    getInitialState() {
        return {
            expandedMessages: Immutable.Set(),
            allStreamsLoaded: false,
            allStreams: Immutable.List()
        };
    },
    componentDidMount() {
        // only load the streams per page
        if (this.state.allStreamsLoaded) {
            return;
        }
        var promise = StreamsStore.listStreams();
        promise.done((streams) => this._onStreamsLoaded(streams));
    },
    _onStreamsLoaded(streams) {
        this.setState({allStreamsLoaded: true, allStreams: Immutable.List(streams).sortBy(stream => stream.title)});
    },

    _toggleMessageDetail(id) {
        var newSet;
        if (this.state.expandedMessages.contains(id)) {
            newSet = this.state.expandedMessages.delete(id);
        } else {
            newSet = this.state.expandedMessages.add(id);
        }
        this.setState({expandedMessages: newSet});
    },

    _fieldColumns() {
        return this.props.selectedFields.delete('message');
    },
    _columnStyle(fieldName) {
        if (fieldName.toLowerCase() === 'source' && this._fieldColumns().size > 1) {
            return {width: 180};
        }
        return {};
    },
    expandAll() {
        var newSet = Immutable.Set(this.props.messages.map((message) => message.id));
        this.setState({expandedMessages: newSet});
    },
    collapseAll() {
        this.setState({expandedMessages: Immutable.Set()});
    },

    render() {
        var selectedColumns = this._fieldColumns();
        return (<div className="content-col">
            <h1 className="pull-left">Messages</h1>

            <ButtonGroup bsSize='small' className="pull-right">
                <Button title="Expand all messages" onClick={this.expandAll}><i className="fa fa-plus"></i></Button>
                <Button title="Collapse all messages"
                        onClick={this.collapseAll}
                        disabled={this.state.expandedMessages.size === 0}><i className="fa fa-minus"></i></Button>
            </ButtonGroup>

            <MessageTablePaginator position="top" currentPage={Number(this.props.page)} resultCount={this.props.resultCount}/>

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
                                                                              expanded={this.state.expandedMessages.contains(message.id)}
                                                                              toggleDetail={this._toggleMessageDetail}
                                                                              inputs={this.props.inputs}
                                                                              streams={this.props.streams}
                                                                              allStreams={this.state.allStreams}
                                                                              allStreamsLoaded={this.state.allStreamsLoaded}
                                                                              nodes={this.props.nodes}
                                                                              highlight={this.props.highlight}
                        />) }
                </table>
            </div>

            <MessageTablePaginator position="bottom" currentPage={Number(this.props.page)} resultCount={this.props.resultCount}>
                <ButtonGroup bsSize='small' className="pull-right" style={{marginTop: 20}}>
                    <Button title="Expand all messages" onClick={this.expandAll}><i className="fa fa-plus"></i></Button>
                    <Button title="Collapse all messages"
                            onClick={this.collapseAll}
                            disabled={this.state.expandedMessages.size === 0}><i className="fa fa-minus"></i></Button>
                </ButtonGroup>
            </MessageTablePaginator>
        </div>);
    }
});

module.exports = ResultTable;