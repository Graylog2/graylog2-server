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
import type { Cell } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import * as React from 'react';
import { useContext } from 'react';
import styled, { css } from 'styled-components';

import type { EntityBase } from 'components/common/EntityDataTable/types';
import DndStylesContext from 'components/common/EntityDataTable/contexts/DndStylesContext';

const Td = styled.td<{ $isDragging: boolean; $transform: string }>(
  ({ $isDragging, $transform }) => css`
    word-break: break-word;
    opacity: ${$isDragging ? 0.4 : 1};
    transform: ${$transform ?? 'none'};
    transition: width transform 0.2s ease-in-out;
  `,
);

const TableCell = <Entity extends EntityBase>({ cell }: { cell: Cell<Entity, unknown> }) => {
  const { columnTransform, activeColId } = useContext(DndStylesContext);
  const isDragging = activeColId === cell.column.id;
  const transform = columnTransform[cell.column.id];

  return (
    <Td $isDragging={isDragging} $transform={transform}>
      {flexRender(cell.column.columnDef.cell, cell.getContext())}
    </Td>
  );
};
export default TableCell;
