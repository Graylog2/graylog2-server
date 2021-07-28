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
import styled, { DefaultTheme } from 'styled-components';

import MessageEventTypesContext, { MessageEventTypesContextType } from 'views/components/contexts/MessageEventTypesContext';
import { Message } from 'views/components/messagelist/Types';
import { MessageEventType } from 'views/types/messageEventTypes';
import { ColorVariants, colorVariants } from 'theme/colors';

import { MessageRow } from './MessageTableEntry';

const getSummaryColor = (theme: DefaultTheme, category: ColorVariants) => {
  if (colorVariants.includes(category)) {
    return theme.colors.variant.darker[category];
  }

  return theme.colors.variant.darker.info;
};

const StyledMessageWrapper = styled.div<{ category: ColorVariants }>(({ theme, category }) => {
  const color = getSummaryColor(theme, category);

  return `
    color: ${color};
    line-height: 1.5em;
    white-space: pre-line;
  `;
});

const getMessageSummary = (messageFields: Message['fields'], messageEvents: MessageEventTypesContextType) => {
  const gl2EventTypeCode = messageFields.gl2_event_type_code;
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
  message: Message,
  onClick?: () => void,
  colSpanFixup?: number,
};

const MessageSummaryRow = ({ message, onClick, colSpanFixup }: Props) => {
  const messageEvents = useContext(MessageEventTypesContext);
  const messageSummary = getMessageSummary(message.fields, messageEvents);

  if (!messageSummary) {
    return null;
  }

  return (
    <MessageRow onClick={onClick} title={messageSummary.template}>
      <td colSpan={colSpanFixup}>
        <StyledMessageWrapper category={messageSummary.category}>
          {messageSummary.summary}
        </StyledMessageWrapper>
      </td>
    </MessageRow>
  );
};

MessageSummaryRow.defaultProps = {
  colSpanFixup: undefined,
  onClick: undefined,
};

export default MessageSummaryRow;
