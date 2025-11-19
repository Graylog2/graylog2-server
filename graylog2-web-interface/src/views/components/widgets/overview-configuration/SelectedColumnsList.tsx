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
import { useCallback, forwardRef, useMemo } from 'react';
import styled from 'styled-components';

import { IconButton, SortableList } from 'components/common';
import UnknownAttributeTitle from 'views/components/widgets/events/UnknownAttributeTitle';

const ListItemContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 3px;
`;

const ColumnTitle = styled.div`
  flex: 1;
`;

type ListItemProps = {
  item: { id: string; title: string };
  dragHandle: React.ReactNode;
  className: string;
  onChange: (columnName: string) => void;
  onRemove: () => void;
  selectedColumns: Array<string>;
};

const ListItem = forwardRef<HTMLDivElement, ListItemProps>(
  ({ className, dragHandle, item, onRemove }: ListItemProps, ref) => (
    <ListItemContainer className={className} ref={ref}>
      {dragHandle}
      <ColumnTitle>{item.title === 'unknown' ? <UnknownAttributeTitle /> : item.title}</ColumnTitle>
      <div>
        <IconButton name="delete" title={`Remove ${item.title} column`} onClick={onRemove} />
      </div>
    </ListItemContainer>
  ),
);

type Props = {
  onChange: (newSelectedColumns: Array<string>) => void;
  displayOverlayInPortal?: boolean;
  selectedColumns: Array<string>;
  columnTitle: (column: string) => string;
};

const SelectedColumnsList = ({ selectedColumns, onChange, displayOverlayInPortal = false, columnTitle }: Props) => {
  const columnsForList = useMemo(
    () => selectedColumns?.map((column) => ({ id: column, title: columnTitle(column) })),
    [columnTitle, selectedColumns],
  );

  const onChangeColumn = useCallback(
    (columnIndex: number, newFieldName: string) => {
      const newColumns = [...selectedColumns];
      newColumns[columnIndex] = newFieldName;

      onChange(newColumns);
    },
    [onChange, selectedColumns],
  );

  const onRemoveColumn = useCallback(
    (removedFieldName: string) => {
      const newColumns = selectedColumns.filter((columnName) => columnName !== removedFieldName);
      onChange(newColumns);
    },
    [onChange, selectedColumns],
  );

  const SortableListItem = useCallback(
    ({ item, index, dragHandle, className, ref }) => (
      <ListItem
        onChange={(newFieldName) => onChangeColumn(index, newFieldName)}
        onRemove={() => onRemoveColumn(item.id)}
        selectedColumns={selectedColumns ?? []}
        item={item}
        dragHandle={dragHandle}
        className={className}
        ref={ref}
      />
    ),
    [selectedColumns, onChangeColumn, onRemoveColumn],
  );

  const onSortChange = useCallback(
    (newColumnsList: Array<{ id: string; title: string }>) => {
      onChange(newColumnsList.map(({ id }) => id));
    },
    [onChange],
  );

  if (!selectedColumns?.length) {
    return null;
  }

  return (
    <SortableList
      items={columnsForList}
      onMoveItem={onSortChange}
      customListItemRender={SortableListItem}
      displayOverlayInPortal={displayOverlayInPortal}
    />
  );
};

export default SelectedColumnsList;
