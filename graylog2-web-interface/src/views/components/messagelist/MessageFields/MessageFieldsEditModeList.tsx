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

import React, { useCallback } from 'react';
import styled, { css } from 'styled-components';

import MessageFieldEditMode from 'views/components/messagelist/MessageFields/MessageFieldEditMode';
import type { MessageFieldsListProps, FormattedField } from 'views/components/messagelist/MessageFields/types';
import { SortableList } from 'components/common';

const Container = styled.div(
  ({ theme }) => css`
    margin-left: ${theme.spacings.sm};
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    padding: ${theme.spacings.xs} 0;
  `,
);

const MessageFieldsEditModeList = ({ fields, message, setFavorites, isFavorite }: MessageFieldsListProps) => {
  const onMoveItem = useCallback(
    (items: Array<FormattedField>) => {
      setFavorites(items.map((item: FormattedField) => item.field));
    },
    [setFavorites],
  );

  const customContentRender = useCallback(
    ({ item: { field, value, type } }) => (
      <MessageFieldEditMode
        key={field}
        fieldName={field}
        fieldType={type}
        message={message}
        value={value}
        isFavorite={isFavorite}
        setFavorites={setFavorites}
      />
    ),
    [isFavorite, message, setFavorites],
  );

  if (!fields.length) return null;

  return (
    <>
      <h3>{isFavorite ? 'Favorites' : 'Details'}</h3>
      <Container>
        <SortableList<FormattedField>
          items={fields}
          onMoveItem={onMoveItem}
          displayOverlayInPortal
          alignItemContent="center"
          customContentRender={customContentRender}
          disableDragging={!isFavorite}
        />
      </Container>
    </>
  );
};

export default MessageFieldsEditModeList;
