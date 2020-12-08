/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { AdditionalContext } from 'views/logic/ActionContext';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { RefreshActions } from 'views/stores/RefreshStore';
import { MessageTableEntry } from 'views/components/messagelist';
import type { BackendMessage, Message } from 'views/components/messagelist/Types';
import FieldSortIcon from 'views/components/widgets/FieldSortIcon';
import Field from 'views/components/Field';

import HighlightMessageContext from '../contexts/HighlightMessageContext';

const TableWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  grid-row: 1;
  -ms-grid-row: 1;
  grid-column: 1;
  -ms-grid-column: 1;
`;

const Table: StyledComponent<{}, ThemeInterface, HTMLTableElement> = styled.table(({ theme }) => css`
  position: relative;
  font-size: ${theme.fonts.size.small};
  margin-top: 0;
  margin-bottom: 60px;
  border-collapse: collapse;
  padding-left: 13px;
  width: 100%;
  word-break: break-all;

  &.messages {
    td,
    th {
      position: relative;
      left: 13px;
    }
  }

  tr {
    border: 0 !important;

    &.fields-row {
      cursor: pointer;

      td {
        padding-top: 10px;
      }
    }

    &.message-row {
      margin-bottom: 5px;
      cursor: pointer;

      td {
        border-top: 0;
        padding-top: 0;
        padding-bottom: 5px;
        font-family: ${theme.fonts.family.monospace};
        color: ${theme.colors.variant.dark.info};
      }

      .message-wrapper {
        line-height: 1.5em;
        white-space: pre-line;
        max-height: 6em; /* show 4 lines: line-height * 4 */
        overflow: hidden;
      }
    }

    &.message-detail-row {
      display: none;

      td {
        padding-top: 5px;
        border-top: 0;
      }

      .row {
        margin-right: 0;
      }

      div[class*="col-"] {
        padding-right: 0;
      }
    }
  }

  tbody {
    &.message-group {
      border-top: 0;
    }

    &.message-group-toggled {
      border-left: 7px solid ${theme.colors.variant.light.info};
    }

    &.message-highlight {
      border-left: 7px solid ${theme.colors.variant.light.success};
    }
  }

  @media print {
    font-size: ${theme.fonts.size.body};
    padding-left: 0;
    min-width: 50%;

    th {
      font-weight: bold !important;
      font-size: inherit !important;
    }

    th,
    td {
      border: 1px ${theme.colors.gray[80]} solid !important;
      left: 0;
      padding: 5px;
      position: static;
    }
  }
`);

const TableHead: StyledComponent<{}, ThemeInterface, HTMLTableSectionElement> = styled.thead(({ theme }) => css`
  background-color: ${theme.colors.gray[90]};
  color: ${theme.utils.readableColor(theme.colors.gray[90])};

  th {
    min-width: 50px;
    border: 0;
    font-size: ${theme.fonts.size.small};
    font-weight: normal;
    white-space: nowrap;
    background-color: ${theme.colors.gray[90]};
    color: ${theme.utils.readableColor(theme.colors.gray[90])};
  }
`);

type State = {
  expandedMessages: Immutable.Set<string>,
};

type Props = {
  activeQueryId: string,
  config: MessagesWidgetConfig,
  editing?: boolean,
  fields: Immutable.List<FieldTypeMapping>,
  messages: Array<BackendMessage>,
  onSortChange: (newSortConfig: SortConfig[]) => Promise<void>,
  selectedFields?: Immutable.Set<string>,
  setLoadingState: (loading: boolean) => void,
};

type DefaultProps = {
  editing: boolean,
  selectedFields: Immutable.Set<string>,
};

class MessageTable extends React.Component<Props, State> {
  static propTypes = {
    activeQueryId: PropTypes.string.isRequired,
    config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
    editing: PropTypes.bool,
    fields: CustomPropTypes.FieldListType.isRequired,
    messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    onSortChange: PropTypes.func.isRequired,
    selectedFields: PropTypes.object,
    setLoadingState: PropTypes.func.isRequired,
  };

  static defaultProps: DefaultProps = {
    editing: false,
    selectedFields: Immutable.Set<string>(),
  };

  constructor(props: Props) {
    super(props);

    this.state = {
      expandedMessages: Immutable.Set<string>(),
    };
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
    return ((fields && fields.find((f) => f.name === fieldName)) || { type: FieldType.Unknown }).type;
  };

  _getFormattedMessages = (): Array<Message> => {
    const { messages } = this.props;

    return messages.map((m) => ({
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
    const { fields, activeQueryId, config, editing, onSortChange, setLoadingState } = this.props;
    const formattedMessages = this._getFormattedMessages();
    const selectedFields = this._getSelectedFields();

    return (
      <TableWrapper className="table-responsive">
        <Table className="table table-condensed">
          <TableHead>
            <tr>
              {selectedFields.toSeq().map((selectedFieldName) => {
                return (
                  <th key={selectedFieldName}
                      style={this._columnStyle(selectedFieldName)}>
                    <Field type={this._fieldTypeFor(selectedFieldName, fields)}
                           name={selectedFieldName}
                           queryId={activeQueryId}>
                      {selectedFieldName}
                    </Field>
                    {editing && (
                      <FieldSortIcon fieldName={selectedFieldName}
                                     onSortChange={onSortChange}
                                     setLoadingState={setLoadingState}
                                     config={config} />
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
                  {(highlightMessageId) => (
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
      </TableWrapper>
    );
  }
}

export default MessageTable;
