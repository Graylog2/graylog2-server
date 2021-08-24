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
import styled, { css } from 'styled-components';

import FieldType from 'views/logic/fieldtypes/FieldType';
import { Message } from 'views/components/messagelist/Types';
import usePluginEntities from 'views/logic/usePluginEntities';
import { MessageEventType } from 'views/types/messageEventTypes';
import MessageFieldRow from 'views/components/messagelist/MessageFieldRow';

const TableRow = styled.tr(({ theme }) => css`
  && {
    margin-bottom: 5px;
    cursor: pointer;
  
    td {
      border-top: 0;
      padding-top: 0;
      padding-bottom: 5px;
      font-family: ${theme.fonts.family.monospace};
      color: ${theme.colors.variant.dark.info};
    }
  }
`);

const getMessageEventType = (eventTypeCode: string | undefined, messageEventTypes: Array<MessageEventType>) => {
  if (!eventTypeCode) {
    return undefined;
  }

  return messageEventTypes?.find((eventType) => eventType.gl2EventTypeCode === eventTypeCode);
};

const renderMessageFieldRow = (message, messageFieldType) => (
  <MessageFieldRow message={message}
                   messageFieldType={messageFieldType} />
);

type Props = {
  onRowClick: () => void,
  colSpanFixup: number,
  message: Message,
  showMessageRow?: boolean,
  showSummary?: boolean,
  messageFieldType: FieldType,
};

const useMessageSummary = (showSummary, message, messageFieldType) => {
  const messageEventTypes = usePluginEntities('messageEventTypes');
  const MessageSummaryComponent = usePluginEntities('views.components.widgets.messageTable.summary')?.[0];

  if (!showSummary || !MessageSummaryComponent) {
    return null;
  }

  const messageEventType = getMessageEventType(message.fields.gl2_event_type_code, messageEventTypes);

  if (!messageEventType) {
    return null;
  }

  return (
    <MessageSummaryComponent messageEventType={messageEventType}
                             messageFields={message.fields}
                             renderFallback={() => renderMessageFieldRow(message, messageFieldType)} />
  );
};

const MessagePreview = ({ showMessageRow, showSummary, onRowClick, colSpanFixup, message, messageFieldType }: Props) => {
  const messageSummary = useMessageSummary(showSummary, message, messageFieldType);

  return (
    <TableRow onClick={onRowClick}>
      <td colSpan={colSpanFixup}>
        {(showMessageRow && !messageSummary) && renderMessageFieldRow(message, messageFieldType)}

        {messageSummary}
      </td>
    </TableRow>
  );
};

MessagePreview.defaultProps = {
  showMessageRow: false,
  showSummary: false,
};

export default MessagePreview;
