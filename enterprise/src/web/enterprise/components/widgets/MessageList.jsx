import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import Immutable from 'immutable';

import { MessageTableEntry } from 'enterprise/components/messagelist';
import { MessageTablePaginator } from 'components/search';
import Field from 'enterprise/components/Field';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import CombinedProvider from 'injection/CombinedProvider';
import { QueriesStore } from 'enterprise/stores/QueriesStore';
import { SelectedFieldsStore } from '../../stores/SelectedFieldsStore';
import { ViewStore } from '../../stores/ViewStore';
import { SearchConfigStore } from '../../stores/SearchConfigStore';

const { ConfigurationActions } = CombinedProvider.get('Configuration');
const RefreshActions = ActionsProvider.getActions('Refresh');

const UniversalSearchStore = StoreProvider.getStore('UniversalSearch');

const MessageList = createReactClass({
  displayName: 'MessageList',

  propTypes: {
    data: PropTypes.shape({
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    }).isRequired,
    filter: PropTypes.string,
    pageSize: PropTypes.number.isRequired,
  },

  mixins: [
    Reflux.connect(SearchConfigStore, 'configurations'),
    Reflux.connect(SelectedFieldsStore, 'selectedFields'),
    Reflux.connect(ViewStore, 'currentView'),
    Reflux.connect(QueriesStore, 'queries'),
  ],

  getDefaultProps() {
    return {
      filter: '',
      pageSize: UniversalSearchStore.DEFAULT_LIMIT,
    };
  },

  getInitialState() {
    return {
      currentPage: 1,
      expandedMessages: Immutable.Set(),
    };
  },

  _columnStyle(fieldName) {
    const selectedFields = Immutable.OrderedSet(this.props.fields);
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

  _parseFilter(filter) {
    const terms = filter.split(/\s+(?:AND|OR)\s+/i);
    return terms.map((term) => {
      const [key, value] = term.split(/\s*:\s*/);
      return { key, value };
    });
  },

  _filterMessages(filter, messages) {
    const filters = this._parseFilter(filter);
    return messages.filter(({ message }) => {
      return filters.every(({ key, value }) => message[key] === value);
    });
  },

  render() {
    const pageSize = this.props.pageSize || 7;
    const messages = this.props.data.messages || [];
    const { fields, filter } = this.props;
    const filteredMessages = filter && filter !== '' ? this._filterMessages(this.props.filter, messages) : messages;
    const messageSlice = filteredMessages
      .slice((this.state.currentPage - 1) * pageSize, this.state.currentPage * pageSize)
      .map((m) => {
        return {
          fields: m.message,
          formatted_fields: m.message,
          id: m.message._id,
          index: m.index,
        };
      });
    const { selectedFields } = this.state;
    const selectedColumns = Immutable.OrderedSet(this._fieldColumns(selectedFields));
    const { activeQuery, view } = this.state.currentView;
    return (
      <span>
        <MessageTablePaginator currentPage={Number(this.state.currentPage)}
          onPageChange={newPage => this.setState({ currentPage: newPage })}
          pageSize={pageSize}
          position="top"
          resultCount={messages.length} />

        <div className="search-results-table" style={{ overflow: 'auto', height: '100%' }}>
          <div className="table-responsive">
            <div className="messages-container">
              <table className="table table-condensed messages">
                <thead>
                  <tr>
                    <th style={{ width: 180 }}><Field interactive name="Timestamp" queryId={activeQuery} /></th>
                    {selectedColumns.toSeq().map((selectedFieldName) => {
                      return (
                        <th key={selectedFieldName}
                          style={this._columnStyle(selectedFieldName)}>
                          <Field interactive
                                 type={fields.find(f => f.get('field_name') === selectedFieldName).get('physical_type', 'unknown')}
                                 name={selectedFieldName}
                                 queryId={activeQuery}
                                 viewId={view.id} />
                        </th>
                      );
                    })}
                  </tr>
                </thead>
                {messageSlice.map((message) => {
                  return (
                    <MessageTableEntry key={`${message.index}-${message.id}`}
                                       fields={fields}
                                       disableSurroundingSearch
                                       message={message}
                                       showMessageRow={selectedFields.contains('message')}
                                       selectedFields={selectedColumns}
                                       expanded={this.state.expandedMessages.contains(`${message.index}-${message.id}`)}
                                       toggleDetail={this._toggleMessageDetail}
                                       inputs={new Immutable.Map()}
                                       streams={new Immutable.Map()}
                                       allStreams={new Immutable.List()}
                                       allStreamsLoaded
                                       nodes={new Immutable.Map()}
                                       highlight={false}
                                       expandAllRenderAsync={false}
                                       searchConfig={this.state.configurations.searchesClusterConfig} />
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
