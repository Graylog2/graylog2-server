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
import { useMemo } from 'react';

import ButtonToolbar from 'components/bootstrap/ButtonToolbar';

import useSelectedEntities from './hooks/useSelectedEntities';
import TableCell from './TableCell';
import type { ColumnRenderersByAttribute, Column, EntityBase } from './types';
import RowCheckbox from './RowCheckbox';

const ActionsCell = styled.td`
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
  actions?: (entity: Entity) => React.ReactNode,
  entityAttributesAreCamelCase: boolean,
  isEntitySelectable: (entity: Entity) => boolean,
};

const TableRow = <Entity extends EntityBase, Meta>({
  columns,
  columnRenderersByAttribute,
  displaySelect,
  displayActions,
  entity,
  actions,
  index,
  actionsRef,
  entityAttributesAreCamelCase,
  isEntitySelectable,
}: Props<Entity>) => {
  const { toggleEntitySelect, selectedEntities } = useSelectedEntities();
  const isSelected = !!selectedEntities?.includes(entity.id);
  const actionButtons = displayActions ? <ButtonToolbar>{actions(entity)}</ButtonToolbar> : null;
  const isSelectDisabled = useMemo(() => !(displaySelect && isEntitySelectable(entity)), [displaySelect, entity, isEntitySelectable]);
  const title = `${isSelected ? 'Deselect' : 'Select'} entity`;

  return (
    <tr>
      {displaySelect && (
        <td aria-label="Select cell">
          <RowCheckbox onChange={() => toggleEntitySelect(entity.id)}
                       title={title}
                       checked={isSelected}
                       disabled={isSelectDisabled}
                       aria-label={title} />
        </td>
      )}
      {columns.map((column) => {
        const columnRenderer = columnRenderersByAttribute[column.id];

        return (
          <TableCell<Entity, Meta> columnRenderer={columnRenderer}
                                   entityAttributesAreCamelCase={entityAttributesAreCamelCase}
                                   entity={entity}
                                   column={column}
                                   key={`${entity.id}-${column.id}`} />
        );
      })}
      {displayActions ? (
        <ActionsCell>
          <ActionsRef ref={index === 0 ? actionsRef : undefined}>{actionButtons}</ActionsRef>
        </ActionsCell>
      ) : null}
    </tr>
  );
};

export default React.memo(TableRow) as typeof TableRow;
