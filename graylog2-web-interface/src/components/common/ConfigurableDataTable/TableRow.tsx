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
import styled from 'styled-components';
import { useCallback } from 'react';

import { TextOverflowEllipsis } from 'components/common';

import TableCell from './TableCell';
import type { CustomCells, Attribute } from './types';
import RowCheckbox from './RowCheckbox';

const ActionsCell = styled.td`
  > div {
    display: flex;
    justify-content: right;
  }
`;

const defaultAttributeCellRenderer = {
  description: {
    renderCell: (listItem) => (
      <TextOverflowEllipsis>
        {listItem.description}
      </TextOverflowEllipsis>
    ),
    maxWidth: '30vw',
  },
};

type Props<ListItem extends { id: string }> = {
  customCells?: CustomCells<ListItem>,
  displayBatchSelectCol: boolean,
  displayRowActions: boolean,
  listItem: ListItem,
  onToggleRowSelect: (itemId: string) => void,
  rowActions?: (listItem: ListItem) => React.ReactNode,
  selectedItemsIds: Array<string>,
  visibleAttributes: Array<Attribute>,
};

const TableRow = <ListItem extends { id: string }>({
  customCells,
  displayBatchSelectCol,
  displayRowActions,
  listItem,
  onToggleRowSelect,
  rowActions,
  selectedItemsIds,
  visibleAttributes,
}: Props<ListItem>) => {
  const isSelected = selectedItemsIds?.includes(listItem.id);
  const toggleRowSelect = useCallback(
    () => onToggleRowSelect(listItem.id),
    [listItem.id, onToggleRowSelect],
  );

  return (
    <tr key={listItem.id}>
      {displayBatchSelectCol && (
        <td style={{ width: '20px' }}>
          <RowCheckbox onChange={toggleRowSelect}
                       title={`${isSelected ? 'Deselect' : 'Select'} row`}
                       checked={isSelected} />
        </td>
      )}
      {visibleAttributes.map((attribute) => {
        const cellRenderer = customCells?.[attribute.id] ?? defaultAttributeCellRenderer[attribute.id];

        return (
          <TableCell cellRenderer={cellRenderer}
                     listItem={listItem}
                     attribute={attribute}
                     key={`${listItem.id}-${attribute.id}`} />
        );
      })}
      {displayRowActions ? <ActionsCell>{rowActions(listItem)}</ActionsCell> : null}
    </tr>
  );
};

TableRow.defaultProps = {
  customCells: undefined,
  rowActions: undefined,
};

export default TableRow;
