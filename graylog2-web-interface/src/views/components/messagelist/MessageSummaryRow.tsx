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
import { TableRow } from 'views/components/messagelist/MessageFieldRow';

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
  onClick?: () => void,
  colSpanFixup?: number,
  messageSummary: {
    template: string,
    category: ColorVariants,
    summary: string,
  },
};

const MessageSummaryRow = ({ message, onClick, colSpanFixup }: Props) => {
  const messageEventTypes = usePluginEntities('messageEventTypes');
  const messageSummary = getMessageSummary(message.fields, messageEventTypes);

  if (!messageSummary) {
    return null;
  }

  return (
    <TableRow onClick={onClick} title={messageSummary.template}>
      <td colSpan={colSpanFixup}>
        <StyledMessageWrapper category={messageSummary.category}>
          {messageSummary.summary}
        </StyledMessageWrapper>
      </td>
    </TableRow>
  );
};

MessageSummaryRow.defaultProps = {
  colSpanFixup: undefined,
  onClick: undefined,
};

export default MessageSummaryRow;
