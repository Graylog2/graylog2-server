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
import styled, { css } from 'styled-components';
import { useCallback } from 'react';

import TableCell from './TableCell';
import type { ColumnRenderers, Column } from './types';
import RowCheckbox from './RowCheckbox';

export const BULK_SELECT_COLUMN_WIDTH = 20;

const ActionsCell = styled.th<{ $width: number | undefined }>(({ $width }) => css`
  width: ${$width ? `${$width}px` : 'auto'};
  text-align: right;

  .btn-toolbar {
    display: inline-flex;
  }
`);

const ActionsRef = styled.div`
  display: inline-flex;
`;

type Props<Entity extends { id: string }> = {
  actionsColWidth: number | undefined,
  actionsRef: React.Ref<HTMLDivElement>
  columns: Array<Column>,
  columnRenderers: ColumnRenderers<Entity>,
  columnsWidths: { [columnId: string]: number },
  displaySelect: boolean,
  displayActions: boolean,
  entity: Entity,
  index: number,
  isSelected: boolean,
  onToggleEntitySelect: (entityId: string) => void,
  rowActions?: (entity: Entity) => React.ReactNode,
};

const TableRow = <Entity extends { id: string }>({
  actionsColWidth,
  columns,
  columnRenderers,
  columnsWidths,
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
        <td style={{ width: BULK_SELECT_COLUMN_WIDTH }}>
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
                     colWidth={columnsWidths[column.id]}
                     key={`${entity.id}-${column.id}`} />
        );
      })}
      {displayActions ? (
        <ActionsCell $width={actionsColWidth}>
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
