// @flow strict
import * as React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
// $FlowFixMe: imports from core need to be fixed in flow
import MessageTablePaginator from 'components/search/MessageTablePaginator';
// $FlowFixMe: imports from core need to be fixed in flow
import CombinedProvider from 'injection/CombinedProvider';
// $FlowFixMe: imports from core need to be fixed in flow
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

import { Messages } from 'enterprise/Constants';
import { MessageTableEntry } from 'enterprise/components/messagelist';
import Field from 'enterprise/components/Field';

import { AdditionalContext } from 'enterprise/logic/ActionContext';
import { SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import CustomPropTypes from 'enterprise/components/CustomPropTypes';
import { ViewStore } from 'enterprise/stores/ViewStore';
import { RefreshActions } from 'enterprise/stores/RefreshStore';
import MessagesWidgetConfig from 'enterprise/logic/widgets/MessagesWidgetConfig';

import styles from './MessageList.css';

const { InputsActions } = CombinedProvider.get('Inputs');

const MessageList = createReactClass({
  displayName: 'MessageList',

  propTypes: {
    fields: CustomPropTypes.FieldListType.isRequired,
    pageSize: PropTypes.number,
    config: PropTypes.instanceOf(MessagesWidgetConfig),
    data: PropTypes.shape({
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    }).isRequired,
    containerHeight: PropTypes.number,
    selectedFields: PropTypes.object,
    currentView: PropTypes.object,
  },

  getDefaultProps() {
    return {
      pageSize: Messages.DEFAULT_LIMIT,
      containerHeight: undefined,
      selectedFields: Immutable.Set(),
      currentView: { view: {}, activeQuery: undefined },
      config: undefined,
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

  _getSelectedFields() {
    const { selectedFields, config } = this.props;
    if (config) {
      return Immutable.Set(config.fields);
    }
    return selectedFields;
  },

  _columnStyle(fieldName) {
    const { fields } = this.props;
    const selectedFields = Immutable.OrderedSet(fields);
    if (fieldName.toLowerCase() === 'source' && selectedFields.size > 1) {
      return { width: 180 };
    }
    return {};
  },

  _toggleMessageDetail(id) {
    let newSet;
    const { expandedMessages } = this.state;
    if (expandedMessages.contains(id)) {
      newSet = expandedMessages.delete(id);
    } else {
      newSet = expandedMessages.add(id);
      RefreshActions.disable();
    }
    this.setState({ expandedMessages: newSet });
  },

  _fieldTypeFor(fieldName, fields) {
    return (fields.find(f => f.name === fieldName) || { type: FieldType.Unknown }).type;
  },

  render() {
    const { containerHeight, data, fields, currentView, pageSize = 7, config } = this.props;
    let maxHeight = '';
    if (containerHeight) {
      maxHeight = containerHeight - 60;
    }
    const messages = (data && data.messages) || [];
    const { currentPage, expandedMessages } = this.state;
    const messageSlice = messages
      .slice((currentPage - 1) * pageSize, currentPage * pageSize)
      .map((m) => {
        return {
          fields: m.message,
          formatted_fields: MessageFieldsFilter.filterFields(m.message),
          id: m.message._id,
          index: m.index,
          highlight_ranges: m.highlight_ranges,
        };
      });
    const selectedFields = this._getSelectedFields();
    const selectedColumns = Immutable.OrderedSet(selectedFields);
    const { activeQuery, view } = currentView;

    return (
      <span>
        <div className={styles.messageListPaginator}>
          <MessageTablePaginator currentPage={Number(currentPage)}
                                 onPageChange={newPage => this.setState({ currentPage: newPage })}
                                 pageSize={pageSize}
                                 position="top"
                                 resultCount={messages.length} />
        </div>

        <div className="search-results-table" style={{ overflow: 'auto', height: 'calc(100% - 60px)', maxHeight: maxHeight }}>
          <div className="table-responsive">
            <div className={`messages-container ${styles.messageListTableHeader}`}>
              <table className="table table-condensed messages" style={{ marginTop: 0 }}>
                <thead>
                  <tr>
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
                    <AdditionalContext.Provider key={messageKey}
                                                value={{ message }}>
                      <MessageTableEntry fields={fields}
                                         disableSurroundingSearch
                                         message={message}
                                         showMessageRow={config && config.showMessageRow}
                                         selectedFields={selectedColumns}
                                         expanded={expandedMessages.contains(messageKey)}
                                         toggleDetail={this._toggleMessageDetail}
                                         highlight
                                         expandAllRenderAsync={false} />
                    </AdditionalContext.Provider>
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

export default connect(MessageList,
  {
    selectedFields: SelectedFieldsStore,
    currentView: ViewStore,
  });
