import type { Cell } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import * as React from 'react';
import { useContext } from 'react';
import styled, { css } from 'styled-components';

import type { EntityBase } from 'components/common/EntityDataTable/types';
import DndStylesContext from 'components/common/EntityDataTable/contexts/DndStylesContext';

const Td = styled.td<{ $isDragging: boolean; $transform: string; $width: number }>(
  ({ $isDragging, $width, $transform }) => css`
    word-break: break-word;
    opacity: ${$isDragging ? 0.4 : 1};
    transform: ${$transform ?? 'none'};
    transition: width transform 0.2s ease-in-out;
    width: ${$width}px;
    z-index: ${$isDragging ? 1 : 0};
  `,
);

const TableCell = <Entity extends EntityBase>({ cell }: { cell: Cell<Entity, unknown> }) => {
  const { columnTransform, activeColId } = useContext(DndStylesContext);
  const isDragging = activeColId === cell.column.id;
  const transform = columnTransform[cell.column.id];

  return (
    <Td $isDragging={isDragging} $width={cell.column.getSize()} $transform={transform}>
      {flexRender(cell.column.columnDef.cell, cell.getContext())}
    </Td>
  );
};
export default TableCell;
