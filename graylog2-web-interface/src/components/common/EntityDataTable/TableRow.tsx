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
import type { Row } from '@tanstack/react-table';
import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';

import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import TableCell from './TableCell';
import type { ColumnRenderersByAttribute, Column, EntityBase } from './types';

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
  actionsRef: React.RefObject<HTMLDivElement>;
  columns: Array<Column>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity>;
  displaySelect: boolean;
  displayActions: boolean;
  row: Row<Entity>;
  index: number;
  actions?: (entity: Entity) => React.ReactNode;

  isEntitySelectable: (entity: Entity) => boolean;
};

const TableRow = <Entity extends EntityBase, Meta>({
  displayActions,
  row,
  actions = undefined,
  index,
  actionsRef,
}: Props<Entity>) => {
  const actionButtons = displayActions ? <ButtonToolbar>{actions(row)}</ButtonToolbar> : null;

  return (
    <tr>
      {row.getVisibleCells().map((cell) => (
        <TableCell<Entity, Meta> cell={cell} key={`${row.id}-${cell.column.id}`} />
      ))}
      {displayActions ? (
        <ActionsCell>
          <ActionsRef ref={index === 0 ? actionsRef : undefined}>{actionButtons}</ActionsRef>
        </ActionsCell>
      ) : null}
    </tr>
  );
};

export default React.memo(TableRow) as typeof TableRow;
