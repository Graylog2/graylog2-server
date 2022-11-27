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
    renderCell: (entity) => (
      <TextOverflowEllipsis>
        {entity.description}
      </TextOverflowEllipsis>
    ),
    maxWidth: '30vw',
  },
};

type Props<Entity extends { id: string }> = {
  customCells?: CustomCells<Entity>,
  displayBulkActionsCol: boolean,
  displayRowActions: boolean,
  isSelected: boolean,
  entity: Entity,
  onToggleRowSelect: (itemId: string) => void,
  rowActions?: (entity: Entity) => React.ReactNode,
  visibleAttributes: Array<Attribute>,
};

const TableRow = <Entity extends { id: string }>({
  customCells,
  displayBulkActionsCol,
  displayRowActions,
  isSelected,
  entity,
  onToggleRowSelect,
  rowActions,
  visibleAttributes,
}: Props<Entity>) => {
  const toggleRowSelect = useCallback(
    () => onToggleRowSelect(entity.id),
    [entity.id, onToggleRowSelect],
  );

  return (
    <tr key={entity.id}>
      {displayBulkActionsCol && (
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
                     entity={entity}
                     attribute={attribute}
                     key={`${entity.id}-${attribute.id}`} />
        );
      })}
      {displayRowActions ? <ActionsCell>{rowActions(entity)}</ActionsCell> : null}
    </tr>
  );
};

TableRow.defaultProps = {
  customCells: undefined,
  rowActions: undefined,
};

export default React.memo(TableRow);
