import React, { PropTypes } from 'react';
import Immutable from 'immutable';
import moment from 'moment';

import { MessageTableEntry, MessageTablePaginator } from 'components/search';
import ActionsProvider from 'injection/ActionsProvider';

const RefreshActions = ActionsProvider.getActions('Refresh');

const MessageList = React.createClass({
  propTypes: {
    data: PropTypes.array.isRequired,
    config: PropTypes.shape({
      fields: PropTypes.arrayOf(PropTypes.string).isRequired,
      pageSize: PropTypes.number,
    }).isRequired,
  },
  getInitialState() {
    return {
      currentPage: 1,
      expandedMessages: Immutable.Set(),
    };
  },
  _columnStyle(fieldName) {
    const selectedFields = Immutable.OrderedSet(this.props.config.fields);
    if (fieldName.toLowerCase() === 'source' && this._fieldColumns(selectedFields).size > 1) {
      return { width: 180 };
    }
    return {};
  },
  _fieldColumns(fields) {
    return fields.delete('message');
  },
  _toggleMessageDetail(id) {
    let newSet;
    if (this.state.expandedMessages.contains(id)) {
      newSet = this.state.expandedMessages.delete(id);
    } else {
      newSet = this.state.expandedMessages.add(id);
      RefreshActions.disable();
    }
    this.setState({ expandedMessages: newSet });
  },

  render() {
    const pageSize = this.props.config.pageSize || 7;
    this.props.data.sort((m1, m2) => moment(m2.message.timestamp).unix() - moment(m1.message.timestamp).unix());
    const messages = this.props.data;
    const messageSlice = messages
      .slice((this.state.currentPage - 1) * pageSize, this.state.currentPage * pageSize)
      .map((m) => {
        return {
          fields: m.message.fields,
          formatted_fields: m.message.formatted_fields || m.message.fields,
          id: m.message.id,
          index: m.index,
        };
      });
    const selectedFields = Immutable.OrderedSet(this.props.config.fields);
    const selectedColumns = this._fieldColumns(selectedFields);
    return (
      <span>
        <MessageTablePaginator currentPage={Number(this.state.currentPage)}
          onPageChange={newPage => this.setState({ currentPage: newPage })}
          pageSize={pageSize}
          position="top"
          resultCount={messages.length} />

        <div className="search-results-table">
          <div className="table-responsive">
            <div className="messages-container">
              <table className="table table-condensed messages">
                <thead>
                  <tr>
                    <th style={{ width: 180 }}>Timestamp</th>
                    {selectedColumns.toSeq().map((selectedFieldName) => {
                      return (
                        <th key={selectedFieldName}
                          style={this._columnStyle(selectedFieldName)}>
                          {selectedFieldName}
                        </th>
                      );
                    })}
                  </tr>
                </thead>
                {messageSlice.map((message) => {
                  return (
                    <MessageTableEntry key={`${message.index}-${message.id}`}
                      disableSurroundingSearch
                      message={message}
                      showMessageRow={selectedFields.contains('message')}
                      selectedFields={selectedColumns}
                      expanded={this.state.expandedMessages.contains(`${message.index}-${message.id}`)}
                      toggleDetail={this._toggleMessageDetail}
                      inputs={new Immutable.Map()}
                      streams={new Immutable.List()}
                      allStreams={new Immutable.List()}
                      allStreamsLoaded
                      nodes={new Immutable.Map()}
                      highlight={false}
                      expandAllRenderAsync={false}
                      searchConfig={{}} />
                  );
                })}
              </table>
            </div>
          </div>
        </div>
      </span>
    );
  },
});

export default MessageList;
