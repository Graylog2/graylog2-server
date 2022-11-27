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

import TableCell from './TableCell';
import type { ColumnRenderers, Column } from './types';
import RowCheckbox from './RowCheckbox';

const ActionsCell = styled.th`
  text-align: right;

  .btn-toolbar {
    display: inline-flex;
  }
`;

const ActionsRef = styled.div`
  display: inline-flex;
`;

const ActionsRef = styled.div`
  display: inline-flex;
`;

type Props<Entity extends { id: string }> = {
  actionsRef: React.RefObject<HTMLDivElement>
  columns: Array<Column>,
  columnRenderers: ColumnRenderers<Entity>,
  displaySelect: boolean,
  displayActions: boolean,
  entity: Entity,
  index: number,
  isSelected: boolean,
  onToggleEntitySelect: (entityId: string) => void,
  rowActions?: (entity: Entity) => React.ReactNode,
};

const TableRow = <Entity extends { id: string }>({
  columns,
  columnRenderers,
  displaySelect,
  displayActions,
  entity,
  isSelected,
  onToggleEntitySelect,
  rowActions,
  index,
  actionsRef,
}: Props<Entity>) => {
  const toggleRowSelect = useCallback(
    () => onToggleEntitySelect(entity.id),
    [entity.id, onToggleEntitySelect],
  );

  return (
    <tr key={entity.id}>
      {displaySelect && (
        <td>
          <RowCheckbox onChange={toggleRowSelect}
                       title={`${isSelected ? 'Deselect' : 'Select'} entity`}
                       checked={isSelected} />
        </td>
      )}
      {columns.map((column) => {
        const columnRenderer = columnRenderers[column.id];

        return (
          <TableCell columnRenderer={columnRenderer}
                     entity={entity}
                     column={column}
                     key={`${entity.id}-${column.id}`} />
        );
      })}
      {displayActions ? (
        <ActionsCell>
          {index === 0 ? <ActionsRef ref={actionsRef}>{rowActions(entity)}</ActionsRef> : rowActions(entity)}
        </ActionsCell>
      ) : null}
    </tr>
  );
};

TableRow.defaultProps = {
  rowActions: undefined,
};

export default React.memo(TableRow);
