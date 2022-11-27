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
import { merge } from 'lodash';

import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';

import TableCell from './TableCell';
import type { ColumnRenderers, Column } from './types';
import RowCheckbox from './RowCheckbox';

const ActionsCell = styled.td`
  > div {
    display: flex;
    justify-content: right;
  }
`;

type Props<Entity extends { id: string }> = {
  columns: Array<Column>,
  customColumnRenderers?: ColumnRenderers<Entity> | undefined,
  displaySelect: boolean,
  displayActions: boolean,
  entity: Entity,
  isSelected: boolean,
  onToggleEntitySelect: (entityId: string) => void,
  rowActions?: (entity: Entity) => React.ReactNode,
};

const TableRow = <Entity extends { id: string }>({
  columns,
  customColumnRenderers,
  displaySelect,
  displayActions,
  entity,
  isSelected,
  onToggleEntitySelect,
  rowActions,
}: Props<Entity>) => {
  const toggleRowSelect = useCallback(
    () => onToggleEntitySelect(entity.id),
    [entity.id, onToggleEntitySelect],
  );

  return (
    <tr key={entity.id}>
      {displaySelect && (
        <td style={{ width: '20px' }}>
          <RowCheckbox onChange={toggleRowSelect}
                       title={`${isSelected ? 'Deselect' : 'Select'} row`}
                       checked={isSelected} />
        </td>
      )}
      {columns.map((column) => {
        const columnRenderer = merge(DefaultColumnRenderers[column.id] ?? {}, customColumnRenderers?.[column.id] ?? {});

        return (
          <TableCell columnRenderer={columnRenderer}
                     entity={entity}
                     column={column}
                     key={`${entity.id}-${column.id}`} />
        );
      })}
      {displayActions ? <ActionsCell>{rowActions(entity)}</ActionsCell> : null}
    </tr>
  );
};

TableRow.defaultProps = {
  customColumnRenderers: undefined,
  rowActions: undefined,
};

export default React.memo(TableRow);
