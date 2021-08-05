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
import { useContext } from 'react';

import FieldType from 'views/logic/fieldtypes/FieldType';
import { Message } from 'views/components/messagelist/Types';
import MessageEventTypesContext, { MessageEventTypesContextType } from 'views/components/contexts/MessageEventTypesContext';
import { MessageEventType } from 'views/types/messageEventTypes';

import MessageSummaryRow from './MessageSummaryRow';
import MessageFieldRow from './MessageFieldRow';

const getMessageSummary = (messageFields: Message['fields'], messageEvents: MessageEventTypesContextType) => {
  const gl2EventTypeCode = messageFields.gl2_event_type_code ?? '100001';
  const eventType: MessageEventType | undefined = messageEvents?.eventTypes?.[gl2EventTypeCode];

  if (!eventType) {
    return undefined;
  }

  const { summaryTemplate: template, category } = eventType;
  const summary = template.replace(/{(\w+)}/g, (fieldNamePlaceholder, fieldName) => messageFields[fieldName] || fieldName);

  return {
    category,
    template,
    summary,
  };
};

type Props = {
  onRowClick: () => void,
  colSpanFixup: number,
  message: Message,
  showMessageRow?: boolean,
  showSummaryRow?: boolean,
  preferSummaryRow?: boolean,
  messageFieldType: FieldType,
};

const MessagePreview = ({ showMessageRow, showSummaryRow, preferSummaryRow, onRowClick, colSpanFixup, message, messageFieldType }: Props) => {
  const messageEvents = useContext(MessageEventTypesContext);
  const messageSummary = (preferSummaryRow || showSummaryRow) ? getMessageSummary(message.fields, messageEvents) : undefined;
  const hasMessageSummary = !!messageSummary;

  return (
    <>
      {showMessageRow && !(preferSummaryRow && hasMessageSummary) && (
        <MessageFieldRow onRowClick={onRowClick}
                         colSpanFixup={colSpanFixup}
                         message={message}
                         messageFieldType={messageFieldType} />
      )}

      {hasMessageSummary && (
        <MessageSummaryRow onClick={onRowClick}
                           colSpanFixup={colSpanFixup}
                           messageSummary={messageSummary} />
      )}
    </>
  );
};

MessagePreview.defaultProps = {
  showMessageRow: false,
  showSummaryRow: false,
  preferSummaryRow: false,
};

export default MessagePreview;
