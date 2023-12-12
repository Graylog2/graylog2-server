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
import * as React from 'react';
import { useCallback, useState, useMemo } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import FieldType from 'views/logic/fieldtypes/FieldType';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { MessageTableEntry } from 'views/components/messagelist';
import type { BackendMessage, Message } from 'views/components/messagelist/Types';
import FieldSortIcon from 'views/components/widgets/FieldSortIcon';
import Field from 'views/components/Field';
import MessageTableProviders from 'views/components/messagelist/MessageTableProviders';
import { VISUALIZATION_TABLE_HEADER_HEIGHT } from 'views/Constants';
import useAutoRefresh from 'views/hooks/useAutoRefresh';

import InteractiveContext from '../contexts/InteractiveContext';

const Table = styled.table(({ theme }) => css`
  position: relative;
  font-size: ${theme.fonts.size.small};
  margin: 0;
  border-collapse: collapse;
  width: 100%;
  word-break: break-all;

  @media print {
    font-size: ${theme.fonts.size.body};
    padding-left: 0;
    min-width: 50%;

    th {
      font-weight: bold !important;
      font-size: inherit !important;
      white-space: break-spaces !important;
      word-break: break-all !important;
    }

    th,
    td {
      border: 1px ${theme.colors.gray[80]} solid !important;
      left: 0;
      padding: 5px !important;
      position: static;
    }
  }
`);

const TableWrapper = styled.div(({ theme }) => css`
  display: flex;
  flex-direction: column;
  overflow: auto;

  /* Fixes overflow of children with position: fixed */
  clip-path: inset(0 0 0 0);

  @media screen and (max-width: ${theme.breakpoints.max.md}) {
    &.table-responsive {
      overflow-y: auto;
    }
  }
`);

const TableHead = styled.thead(({ theme }) => css`
  background-color: ${theme.colors.gray[90]};
  color: ${theme.utils.readableColor(theme.colors.gray[90])};
  position: sticky;
  top: 0;
  z-index: 1;
  
  && > tr > th {
    min-width: 50px;
    height: ${VISUALIZATION_TABLE_HEADER_HEIGHT}px;
    padding: 0 5px;
    vertical-align: center;
    border: 0;
    font-size: ${theme.fonts.size.small};
    font-weight: normal;
    white-space: nowrap;
    background-color: ${theme.colors.gray[90]};
    color: ${theme.utils.readableColor(theme.colors.gray[90])};
  }
`);

type Props = {
  activeQueryId: string,
  config: MessagesWidgetConfig,
  fields: Immutable.List<FieldTypeMapping>,
  messages: Array<BackendMessage>,
  onSortChange: (newSortConfig: SortConfig[]) => Promise<void>,
  scrollContainerRef: React.MutableRefObject<HTMLDivElement>,
  setLoadingState: (loading: boolean) => void,
};

const _fieldTypeFor = (fieldName: string, fields: Immutable.List<FieldTypeMapping>) => ((fields
  && fields.find((f) => f.name === fieldName)) || { type: FieldType.Unknown }).type;

const _getFormattedMessages = (messages): Array<Message> => messages.map((m) => ({
  fields: m.message,
  formatted_fields: MessageFieldsFilter.filterFields(m.message),
  id: m.message._id,
  index: m.index,
  highlight_ranges: m.highlight_ranges,
  decoration_stats: m.decoration_stats,
}));

const _toggleMessageDetail = (
  id: string,
  expandedMessages: Immutable.Set<string>,
  setExpandedMessages: (newValue: Immutable.Set<string>) => void,
  stopAutoRefresh: () => void,
) => {
  let newSet;

  if (expandedMessages.contains(id)) {
    newSet = expandedMessages.delete(id);
  } else {
    newSet = expandedMessages.add(id);
    stopAutoRefresh();
  }

  setExpandedMessages(newSet);
};

const MessageTable = ({ fields, activeQueryId, messages, config, onSortChange, setLoadingState, scrollContainerRef }: Props) => {
  const { stopAutoRefresh } = useAutoRefresh();
  const [expandedMessages, setExpandedMessages] = useState(Immutable.Set<string>());
  const formattedMessages = useMemo(() => _getFormattedMessages(messages), [messages]);
  const selectedFields = useMemo(() => Immutable.OrderedSet<string>(config?.fields ?? []), [config?.fields]);
  const toggleDetail = useCallback((id: string) => _toggleMessageDetail(id, expandedMessages, setExpandedMessages, stopAutoRefresh), [expandedMessages, stopAutoRefresh]);

  return (
    <MessageTableProviders>
      <TableWrapper className="table-responsive" id="sticky-augmentations-container" ref={scrollContainerRef}>
        <Table className="table table-condensed">
          <TableHead>
            <tr>
              {selectedFields.toSeq().map((selectedFieldName) => (
                <th key={selectedFieldName}>
                  <Field type={_fieldTypeFor(selectedFieldName, fields)}
                         name={selectedFieldName}
                         queryId={activeQueryId}>
                    {selectedFieldName}
                  </Field>
                  <InteractiveContext.Consumer>
                    {(interactive) => (interactive && (
                      <FieldSortIcon fieldName={selectedFieldName}
                                     onSortChange={onSortChange}
                                     setLoadingState={setLoadingState}
                                     config={config} />
                    ))}
                  </InteractiveContext.Consumer>
                </th>
              )).toArray()}
            </tr>
          </TableHead>
          {formattedMessages.map((message) => {
            const messageKey = `${message.index}-${message.id}`;

            return (
              <MessageTableEntry fields={fields}
                                 key={messageKey}
                                 message={message}
                                 config={config}
                                 showMessageRow={config?.showMessageRow}
                                 selectedFields={selectedFields}
                                 expanded={expandedMessages.contains(messageKey)}
                                 toggleDetail={toggleDetail}
                                 expandAllRenderAsync={false} />
            );
          })}
        </Table>
      </TableWrapper>
    </MessageTableProviders>
  );
};

MessageTable.propTypes = {
  activeQueryId: PropTypes.string.isRequired,
  config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  messages: PropTypes.arrayOf(PropTypes.object).isRequired,
  onSortChange: PropTypes.func.isRequired,
  setLoadingState: PropTypes.func.isRequired,
};

export default React.memo(MessageTable);
