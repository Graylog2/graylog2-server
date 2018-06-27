import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import Immutable from 'immutable';

import { MessageTableEntry } from 'enterprise/components/messagelist';
import { MessageTablePaginator } from 'components/search';
import Field from 'enterprise/components/Field';

import CombinedProvider from 'injection/CombinedProvider';
import { QueriesStore } from 'enterprise/stores/QueriesStore';
import { SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import { ViewStore } from 'enterprise/stores/ViewStore';
import { SearchConfigStore } from 'enterprise/stores/SearchConfigStore';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import { StreamsStore } from 'enterprise/stores/StreamsStore';

import styles from './MessageList.css';
import MessageFormatter from '../../logic/messages/MessageFormatter';

const { InputsActions, InputsStore } = CombinedProvider.get('Inputs');
const { NodesStore } = CombinedProvider.get('Nodes');
const { RefreshActions } = CombinedProvider.get('Refresh');
const { UniversalSearchStore } = CombinedProvider.get('UniversalSearch');

const MessageList = createReactClass({
  displayName: 'MessageList',

  propTypes: {
    data: PropTypes.shape({
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    }).isRequired,
    fields: PropTypes.arrayOf(PropTypes.object).isRequired,
    filter: PropTypes.string,
    pageSize: PropTypes.number.isRequired,
  },

  mixins: [
    Reflux.connect(InputsStore, 'inputs'),
    Reflux.connect(NodesStore, 'nodes'),
    Reflux.connect(SearchConfigStore, 'configurations'),
    Reflux.connect(SelectedFieldsStore, 'selectedFields'),
    Reflux.connect(StreamsStore, 'availableStreams'),
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

  componentDidMount() {
    InputsActions.list();
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

  _fieldTypeFor(fieldName, fields) {
    return (fields.find(f => f.name === fieldName) || { type: FieldType.Unknown }).type;
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
          formatted_fields: MessageFormatter(m.message),
          id: m.message._id,
          index: m.index,
        };
      });
    const { availableStreams, selectedFields } = this.state;
    const { inputs } = this.state.inputs || { inputs: [] };
    const inputsMap = Immutable.Map(inputs.map(input => [input.id, input]));
    const { nodes } = this.state.nodes;
    const nodesMap = Immutable.Map(nodes);
    const selectedColumns = Immutable.OrderedSet(this._fieldColumns(selectedFields));
    const { activeQuery, view } = this.state.currentView;
    const { streams } = availableStreams;
    const streamsMap = Immutable.Map(streams.map(stream => [stream.id, stream]));
    const allStreams = Immutable.List(streams);
    return (
      <span>
        <div className={styles.messageListPaginator}>
          <MessageTablePaginator currentPage={Number(this.state.currentPage)}
            onPageChange={newPage => this.setState({ currentPage: newPage })}
            pageSize={pageSize}
            position="top"
            resultCount={messages.length} />
        </div>

        <div className="search-results-table" style={{ overflow: 'auto', height: '100%' }}>
          <div className="table-responsive">
            <div className={`messages-container ${styles.messageListTableHeader}`}>
              <table className="table table-condensed messages" style={{ marginTop: 0 }}>
                <thead>
                  <tr>
                    <th style={{ width: 180 }}><Field interactive name="Timestamp" queryId={activeQuery} type={this._fieldTypeFor('timestamp', fields)} /></th>
                    {selectedColumns.toSeq().map((selectedFieldName) => {
                      return (
                        <th key={selectedFieldName}
                            style={this._columnStyle(selectedFieldName)}>
                          <Field interactive
                                 type={this._fieldTypeFor(selectedFieldName, fields)}
                                 name={selectedFieldName}
                                 queryId={activeQuery}
                                 viewId={view.id} />
                        </th>
                      );
                    })}
                  </tr>
                </thead>
                {messageSlice.map((message) => {
                  const messageKey = `${message.index}-${message.id}`;
                  return (
                    <MessageTableEntry key={messageKey}
                                       fields={fields}
                                       disableSurroundingSearch
                                       message={message}
                                       showMessageRow={selectedFields.contains('message')}
                                       selectedFields={selectedColumns}
                                       expanded={this.state.expandedMessages.contains(messageKey)}
                                       toggleDetail={this._toggleMessageDetail}
                                       inputs={inputsMap}
                                       streams={streamsMap}
                                       allStreams={allStreams}
                                       allStreamsLoaded
                                       nodes={nodesMap}
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
