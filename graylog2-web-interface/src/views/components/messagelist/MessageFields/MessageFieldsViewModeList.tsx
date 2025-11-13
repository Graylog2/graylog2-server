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

import React, { useContext } from 'react';
import styled, { css } from 'styled-components';

import type { MessageFieldsListProps } from 'views/components/messagelist/MessageFields/types';
import MessageField from 'views/components/messagelist/MessageField';
import { MessageDetailsDefinitionList } from 'components/common';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';

export const MessageDetailsDL = styled(MessageDetailsDefinitionList)(
  ({ theme }) => css`
    color: ${theme.colors.text.primary};

    dd {
      &:not(:last-child) {
        border-bottom: 1px solid ${theme.colors.table.row.divider};
      }
    }
  `,
);

const MessageFieldsViewModeList = ({ fields, isFavorite = false }: MessageFieldsListProps) => {
  const { message } = useContext(MessageFavoriteFieldsContext);
  if (!fields.length) return null;

  return (
    <div data-testid={`${isFavorite ? 'favorite' : 'rest'}-fields-list`}>
      <MessageDetailsDL className="message-details-fields">
        {fields.map(({ field, value, type }) => (
          <MessageField key={field} fieldName={field} fieldType={type} value={value} message={message} />
        ))}
      </MessageDetailsDL>
    </div>
  );
};

export default MessageFieldsViewModeList;
