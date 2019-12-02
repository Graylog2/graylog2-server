// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { AdditionalContext } from 'views/logic/ActionContext';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';

import { RefreshActions } from 'views/stores/RefreshStore';

import { MessageTableEntry } from 'views/components/messagelist';
import Field from 'views/components/Field';

const Table = styled.table`
  position: relative;
  font-size: 11px;
  margin-top: 15px;
  margin-bottom: 60px;
  border-collapse: collapse;
  padding-left: 13px;
  width: 100%;
  word-break: break-all;

  table.messages td, table.messages th {
    position: relative;
    left: 13px;
  }

  tr {
    border: 0 !important;
  }

  tbody.message-group {
    border-top: 0;
  }

  tbody.message-group-toggled {
    border-left: 7px solid #16ace3;
  }

  tbody.message-highlight {
    border-left: 7px solid #8dc63f;
  }

  tr.fields-row {
    cursor: pointer;
  }

  tr.fields-row td {
    padding-top: 10px;
  }

  tr.message-row td {
    border-top: 0;
    padding-top: 0;
    padding-bottom: 5px;
    font-family: monospace;
    color: #16ace3;
  }

  tr.message-row {
    margin-bottom: 5px;
    cursor: pointer;
  }

  tr.message-row .message-wrapper {
    line-height: 1.5em;
    white-space: pre-line;
    max-height: 6em; /* show 4 lines: line-height * 4 */
    overflow: hidden;
  }

  tr.message-detail-row {
    display: none;
  }

  tr.message-detail-row td {
    padding-top: 5px;
    border-top: 0;
  }

  tr.message-detail-row .row {
    margin-right: 0;
  }

  tr.message-detail-row div[class*="col-"] {
    padding-right: 0;
  }
`;

const TableHead = styled.thead`
  &&, th {
    background-color: #eee;
    color: #333;
  }

  th {
    border: 0;
    font-size: 11px;
    font-weight: normal;
    white-space: nowrap;
  }
`;

type State = {
  expandedMessages: Immutable.Set,
}

type Props = {
  fields: {},
  config?: MessagesWidgetConfig,
  selectedFields?: {},
  activeQueryId: string,
  messages: Array<Object>
};

class MessageTable extends React.Component<Props, State> {
  static propTypes = {
    activeQueryId: PropTypes.string.isRequired,
    config: CustomPropTypes.instanceOf(MessagesWidgetConfig),
    fields: CustomPropTypes.FieldListType.isRequired,
    messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    selectedFields: PropTypes.object,
  }

  static defaultProps = {
    config: undefined,
    selectedFields: Immutable.Set(),
  };

  state = {
    expandedMessages: Immutable.Set(),
  };

  _columnStyle = (fieldName: string) => {
    const { fields } = this.props;
    const selectedFields = Immutable.OrderedSet(fields);
    if (fieldName.toLowerCase() === 'source' && selectedFields.size > 1) {
      return { width: 180 };
    }
    return {};
  };

  _fieldTypeFor = (fieldName: string, fields: Immutable.List) => {
    return (fields.find(f => f.name === fieldName) || { type: FieldType.Unknown }).type;
  };

  _getFormattedMessages = (): Array<Object> => {
    const { messages } = this.props;
    return messages.map(m => ({
      fields: m.message,
      formatted_fields: MessageFieldsFilter.filterFields(m.message),
      id: m.message._id,
      index: m.index,
      highlight_ranges: m.highlight_ranges,
    }));
  };

  _getSelectedFields = () => {
    const { selectedFields, config } = this.props;
    if (config) {
      return Immutable.Set(config.fields);
    }
    return selectedFields;
  };

  _toggleMessageDetail = (id: string) => {
    let newSet;
    const { expandedMessages } = this.state;
    if (expandedMessages.contains(id)) {
      newSet = expandedMessages.delete(id);
    } else {
      newSet = expandedMessages.add(id);
      RefreshActions.disable();
    }
    this.setState({ expandedMessages: newSet });
  };

  render() {
    const { expandedMessages } = this.state;
    const { fields, activeQueryId, config } = this.props;
    const formattedMessages = this._getFormattedMessages();
    const selectedFields = this._getSelectedFields();
    const selectedColumns = Immutable.OrderedSet(selectedFields);
    return (
      <div className="search-results-table">
        <div className="table-responsive">
          <Table className="table table-condensed" style={{ marginTop: 0 }}>
            <TableHead>
              <tr>
                {selectedColumns.toSeq().map((selectedFieldName) => {
                  return (
                    <th key={selectedFieldName}
                        style={this._columnStyle(selectedFieldName)}>
                      <Field type={this._fieldTypeFor(selectedFieldName, fields)}
                             name={selectedFieldName}
                             queryId={activeQueryId} />
                    </th>
                  );
                })}
              </tr>
            </TableHead>
            {formattedMessages.map((message) => {
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
          </Table>
        </div>
      </div>
    );
  }
}

export default MessageTable;
