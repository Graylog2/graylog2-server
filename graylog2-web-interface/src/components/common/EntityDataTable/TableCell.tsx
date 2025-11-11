import type { Cell } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import * as React from 'react';
import type { Transform } from '@dnd-kit/utilities';
import { useSortable } from '@dnd-kit/sortable';
import styled, { css } from 'styled-components';
import { CSS } from '@dnd-kit/utilities';

import type { EntityBase, ColumnMetaContext } from 'components/common/EntityDataTable/types';

const Td = styled.td<{ $isDragging: boolean; $transform: Transform; $width: number }>(
  ({ $isDragging, $width, $transform }) => css`
    word-break: break-word;
    opacity: ${$isDragging ? 0.8 : 1};
    transform: ${CSS.Translate.toString($transform)};
    transition: width transform 0.2s ease-in-out;
    width: ${$width}px;
    z-index: ${$isDragging ? 1 : 0};
  `,
);

const TableCell = <Entity extends EntityBase>({ cell }: { cell: Cell<Entity, unknown> }) => {
  const columnMeta = cell.column.columnDef.meta as ColumnMetaContext<Entity>;

  const { isDragging, setNodeRef, transform } = useSortable({
    id: cell.column.id,
    disabled: !columnMeta?.enableColumnOrdering,
  });

  return (
    <Td ref={setNodeRef} $isDragging={isDragging} $transform={transform} $width={cell.column.getSize()}>
      {flexRender(cell.column.columnDef.cell, cell.getContext())}
    </Td>
  );
};

export default TableCell;
