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
import { useCallback, useMemo } from 'react';

import ButtonToolbar from 'components/bootstrap/ButtonToolbar';

import TableCell from './TableCell';
import type { ColumnRenderersByAttribute, Column, EntityBase } from './types';
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

type Props<Entity extends EntityBase> = {
  actionsRef: React.RefObject<HTMLDivElement>
  columns: Array<Column>,
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity>,
  displaySelect: boolean,
  displayActions: boolean,
  entity: Entity,
  index: number,
  isSelected: boolean,
  onToggleEntitySelect: (entityId: string) => void,
  rowActions?: (entity: Entity) => React.ReactNode,
  entityAttributesAreCamelCase: boolean,
  isEntitySelectable: (entity: Entity) => boolean
};

const TableRow = <Entity extends EntityBase>({
  columns,
  columnRenderersByAttribute,
  displaySelect,
  displayActions,
  entity,
  isSelected,
  onToggleEntitySelect,
  rowActions,
  index,
  actionsRef,
  entityAttributesAreCamelCase,
  isEntitySelectable,
}: Props<Entity>) => {
  const toggleRowSelect = useCallback(
    () => onToggleEntitySelect(entity.id),
    [entity.id, onToggleEntitySelect],
  );

  const actionButtons = displayActions ? <ButtonToolbar>{rowActions(entity)}</ButtonToolbar> : null;

  const isSelectDisabled = useMemo(() => !(displaySelect && isEntitySelectable(entity)), [displaySelect, entity, isEntitySelectable]);

  return (
    <tr>
      {displaySelect && (
        <td>
          <RowCheckbox onChange={toggleRowSelect}
                       title={`${isSelected ? 'Deselect' : 'Select'} entity`}
                       checked={isSelected}
                       disabled={isSelectDisabled} />
        </td>
      )}
      {columns.map((column) => {
        const columnRenderer = columnRenderersByAttribute[column.id];

        return (
          <TableCell columnRenderer={columnRenderer}
                     entityAttributesAreCamelCase={entityAttributesAreCamelCase}
                     entity={entity}
                     column={column}
                     key={`${entity.id}-${column.id}`} />
        );
      })}
      {displayActions ? (
        <ActionsCell>
          {index === 0 ? <ActionsRef ref={actionsRef}>{actionButtons}</ActionsRef> : actionButtons}
        </ActionsCell>
      ) : null}
    </tr>
  );
};

TableRow.defaultProps = {
  rowActions: undefined,
};

export default React.memo(TableRow);
