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
import styled, { DefaultTheme } from 'styled-components';

import { Message } from 'views/components/messagelist/Types';
import { MessageEventType } from 'views/types/messageEventTypes';
import { ColorVariants, colorVariants } from 'theme/colors';
import usePluginEntities from 'views/logic/usePluginEntities';

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

const getMessageEventType = (gl2EventTypeCode: string, messageEventTypes: Array<MessageEventType> = []) => {
  if (!gl2EventTypeCode) {
    return undefined;
  }

  return messageEventTypes.find((eventType) => (
    eventType.gl2EventTypeCode === gl2EventTypeCode
  ));
};

const getMessageSummary = (messageFields: Message['fields'], messageEventTypes: Array<MessageEventType>) => {
  const messageEventType = getMessageEventType(messageFields.gl2_event_type_code, messageEventTypes);

  if (!messageEventType) {
    return undefined;
  }

  const { summaryTemplate: template, category } = messageEventType;
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
  const messageEventTypes = usePluginEntities('messageEventTypes');
  const messageSummary = getMessageSummary(message.fields, messageEventTypes);

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
