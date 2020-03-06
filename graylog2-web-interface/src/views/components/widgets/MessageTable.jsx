// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled, { css, type StyledComponent } from 'styled-components';
import { isEmpty } from 'lodash';

import { AdditionalContext } from 'views/logic/ActionContext';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';
import Direction from 'views/logic/aggregationbuilder/Direction';
import { Icon } from 'components/common';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import { RefreshActions } from 'views/stores/RefreshStore';

import { MessageTableEntry } from 'views/components/messagelist';
import Field from 'views/components/Field';
import HighlightMessageContext from '../contexts/HighlightMessageContext';

const Table = styled.table`
  position: relative;
  font-size: 11px;
  margin-top: 0;
  margin-bottom: 60px;
  border-collapse: collapse;
  padding-left: 13px;
  width: 100%;
  word-break: break-all;

  table.messages td,
  table.messages th {
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

  @media print {
    font-size: 14px;
    padding-left: 0;
    min-width: 50%;

    th {
      font-weight: bold !important;
      font-size: inherit !important;
    }

    th,
    td {
      border: 1px #ccc solid !important;
      left: 0;
      padding: 5px;
      position: static;
    }
  }
`;

const SortIcon: StyledComponent<{fieldSortActive: boolean}, {}, HTMLButtonElement> = styled.button(({ fieldSortActive }) => {
  const color = fieldSortActive ? '#333' : '#bdbdbd';

  return css`
    border: 0;
    background: transparent;
    color: ${color};

    padding: 5px;

    cursor: pointer;
  `;
});

const TableHead = styled.thead`
  background-color: #eee;
  color: #333;

  th {
    border: 0;
    font-size: 11px;
    font-weight: normal;
    white-space: nowrap;
    background-color: #eee;
    color: #333;
  }
`;

type State = {
  expandedMessages: Immutable.Set<string>,
}

type Props = {
  activeQueryId: string,
  config: MessagesWidgetConfig,
  editing?: boolean,
  fields: Immutable.List<FieldTypeMapping>,
  messages: Array<Object>,
  onConfigChange: (MessagesWidgetConfig) => Promise<void>,
  selectedFields?: Immutable.Set<string>,
  setLoadingState: (loading: boolean) => void,
};

type DefaultProps = {
  selectedFields: Immutable.Set<string>,
};

class MessageTable extends React.Component<Props, State> {
  static propTypes = {
    activeQueryId: PropTypes.string.isRequired,
    config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
    editing: PropTypes.bool,
    fields: CustomPropTypes.FieldListType.isRequired,
    messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    onConfigChange: PropTypes.func.isRequired,
    selectedFields: PropTypes.object,
    setLoadingState: PropTypes.func.isRequired,
  };

  static defaultProps: DefaultProps = {
    editing: false,
    selectedFields: Immutable.Set<string>(),
  };

  state = {
    expandedMessages: Immutable.Set<string>(),
  };

  _changeSort = (config: MessagesWidgetConfig, fieldName: string, fieldSortDirection: ?Direction) => {
    const { onConfigChange, setLoadingState } = this.props;
    let newSort;

    setLoadingState(true);

    switch (fieldSortDirection) {
      case Direction.Ascending:
        newSort = config.sort.map(sort => sort.toBuilder().direction(Direction.Descending).build());
        break;
      case Direction.Descending:
        newSort = config.sort.map(sort => sort.toBuilder().direction(Direction.Ascending).build());
        break;
      default:
        newSort = [new SortConfig('pivot', fieldName, Direction.Ascending)];
        break;
    }

    const newConfig = config.toBuilder().sort(newSort).build();
    onConfigChange(newConfig).then(() => {
      setLoadingState(false);
    });
  }

  _columnStyle = (fieldName: string) => {
    const { fields } = this.props;
    const selectedFields = Immutable.OrderedSet<string>(fields);
    if (fieldName.toLowerCase() === 'source' && selectedFields.size > 1) {
      return { width: 180 };
    }
    return {};
  };

  _fieldTypeFor = (fieldName: string, fields: Immutable.List<FieldTypeMapping>) => {
    return ((fields && fields.find(f => f.name === fieldName)) || { type: FieldType.Unknown }).type;
  };

  _fieldSortDirection = (config: MessagesWidgetConfig, fieldName: string) => {
    const currentSort = !isEmpty(config.sort) ? config.sort[0] : null;
    if (currentSort && currentSort.field === fieldName) {
      return currentSort.direction;
    }
    return undefined;
  }

  _fieldSortDescription = (fielName: string, fieldSortDirection?: Direction) => {
    let newDirection;
    if (fieldSortDirection && fieldSortDirection.direction === Direction.Ascending) {
      newDirection = Direction.Descending.direction;
    } else {
      newDirection = Direction.Ascending.direction;
    }
    return `Sort ${fielName} ${newDirection}`;
  }

  _fieldSortIcon = (fieldSortDirection?: string) => {
    if (fieldSortDirection && fieldSortDirection === Direction.Descending) {
      return 'sort-amount-desc';
    }
    return 'sort-amount-asc';
  }

  _getFormattedMessages = (): Array<Object> => {
    const { messages } = this.props;
    return messages.map(m => ({
      fields: m.message,
      formatted_fields: MessageFieldsFilter.filterFields(m.message),
      id: m.message._id,
      index: m.index,
      highlight_ranges: m.highlight_ranges,
      decoration_stats: m.decoration_stats,
    }));
  };

  _getSelectedFields = (): Immutable.OrderedSet<string> => {
    const { selectedFields, config } = this.props;
    return Immutable.OrderedSet<string>(config ? config.fields : (selectedFields || Immutable.Set<string>()));
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
    const { fields, activeQueryId, config, editing } = this.props;
    const formattedMessages = this._getFormattedMessages();
    const selectedFields = this._getSelectedFields();
    return (
      <div className="table-responsive">
        <Table className="table table-condensed">
          <TableHead>
            <tr>
              {selectedFields.toSeq().map((selectedFieldName) => {
                const fieldSortDirection = this._fieldSortDirection(config, selectedFieldName);
                const fieldSortDescription = this._fieldSortDescription(selectedFieldName, fieldSortDirection);
                return (
                  <th key={selectedFieldName}
                      style={this._columnStyle(selectedFieldName)}>
                    <Field type={this._fieldTypeFor(selectedFieldName, fields)}
                           name={selectedFieldName}
                           queryId={activeQueryId} />
                    {editing && (
                      <SortIcon fieldSortActive={!!fieldSortDirection}
                                title={fieldSortDescription}
                                aria-label={fieldSortDescription}
                                onClick={() => this._changeSort(config, selectedFieldName, fieldSortDirection)}>
                        <Icon name={this._fieldSortIcon(fieldSortDirection)} />
                      </SortIcon>
                    )}
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
                <HighlightMessageContext.Consumer>
                  {highlightMessageId => (
                    <MessageTableEntry fields={fields}
                                       message={message}
                                       showMessageRow={config && config.showMessageRow}
                                       selectedFields={selectedFields}
                                       expanded={expandedMessages.contains(messageKey)}
                                       toggleDetail={this._toggleMessageDetail}
                                       highlightMessage={highlightMessageId}
                                       highlight
                                       expandAllRenderAsync={false} />
                  )}
                </HighlightMessageContext.Consumer>
              </AdditionalContext.Provider>
            );
          })}
        </Table>
      </div>
    );
  }
}

export default MessageTable;
