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

import MessageFieldEditModeListItem from 'views/components/messagelist/MessageFields/MessageFieldEditModeListItem';
import type { MessageFieldsListProps, FormattedField } from 'views/components/messagelist/MessageFields/types';
import { SortableList } from 'components/common';
import useFormattedFields from 'views/components/messagelist/MessageFields/hooks/useFormattedFields';

const Container = styled.div(
  ({ theme }) => css`
    margin-left: ${theme.spacings.sm};
    display: flex;
    flex-direction: column;
    padding: ${theme.spacings.xs} 0;
  `,
);

const ItemContainer = styled.div(
  ({ theme }) => css`
    &:not(:last-child) {
      border-bottom: 1px solid ${theme.colors.table.row.divider};
    }
    padding: ${theme.spacings.xs} ${theme.spacings.sm};
  `,
);

const MessageFieldsEditModeFavoritesList = ({
  fields,
  onFavoriteToggle,
  reorderFavoriteFields,
}: MessageFieldsListProps) => {
  const customContentRender = useCallback(
    ({ item: { field, value, type } }) => (
      <MessageFieldEditModeListItem
        key={field}
        fieldName={field}
        fieldType={type}
        value={value}
        isFavorite
        onFavoriteToggle={onFavoriteToggle}
      />
    ),
    [onFavoriteToggle],
  );

  if (!fields.length) return null;

  return (
    <>
      <h3>Favorite fields</h3>
      <Container>
        <SortableList<FormattedField>
          items={fields}
          onMoveItem={reorderFavoriteFields}
          displayOverlayInPortal
          alignItemContent="center"
          customContentRender={customContentRender}
        />
      </Container>
    </>
  );
};

const MessageFieldsEditModeRestList = ({ fields, onFavoriteToggle }: MessageFieldsListProps) => {
  if (!fields.length) return null;

  return (
    <>
      <h3>Remaining fields</h3>
      <Container>
        {fields.map(({ field, type, value }) => (
          <ItemContainer key={field}>
            <MessageFieldEditModeListItem
              fieldName={field}
              fieldType={type}
              value={value}
              isFavorite={false}
              onFavoriteToggle={onFavoriteToggle}
            />
          </ItemContainer>
        ))}
      </Container>
    </>
  );
};

type Props = {
  reorderFavoriteFields: MessageFieldsListProps['reorderFavoriteFields'];
  onFavoriteToggle: MessageFieldsListProps['onFavoriteToggle'];
  editingFavoriteFields: Array<string>;
};

const MessageFieldsEditModeLists = ({ reorderFavoriteFields, onFavoriteToggle, editingFavoriteFields }: Props) => {
  const { formattedFavorites, formattedRest } = useFormattedFields(editingFavoriteFields);

  return (
    <>
      <MessageFieldsEditModeFavoritesList
        reorderFavoriteFields={reorderFavoriteFields}
        onFavoriteToggle={onFavoriteToggle}
        fields={formattedFavorites}
      />
      <MessageFieldsEditModeRestList onFavoriteToggle={onFavoriteToggle} fields={formattedRest} />
    </>
  );
};

export default MessageFieldsEditModeLists;
