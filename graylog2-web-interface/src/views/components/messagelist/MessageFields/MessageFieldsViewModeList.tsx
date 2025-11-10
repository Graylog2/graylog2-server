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
import styled, { css } from 'styled-components';

import type { MessageFieldsListProps } from 'views/components/messagelist/MessageFields/types';
import MessageField from 'views/components/messagelist/MessageField';
import { MessageDetailsDL } from 'views/components/messagelist/MessageFields/MessageFields';

const Container = styled.div(
  ({ theme }) => css`
    margin-left: ${theme.spacings.sm};
  `,
);

const MessageFieldsViewModeList = ({ fields, message, isFavorite }: MessageFieldsListProps) => {
  if (!fields.length) return null;

  return (
    <>
      <h6>{isFavorite ? 'Favorites' : 'Details'}</h6>
      <Container>
        <MessageDetailsDL className="message-details-fields">
          {fields.map(({ field, value, type }) => (
            <MessageField key={field} fieldName={field} fieldType={type} message={message} value={value} />
          ))}
        </MessageDetailsDL>
      </Container>
    </>
  );
};

export default MessageFieldsViewModeList;
