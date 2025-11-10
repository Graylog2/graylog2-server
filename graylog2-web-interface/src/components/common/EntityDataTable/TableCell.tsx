import type { Cell } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import type { CSSProperties } from 'react';
import * as React from 'react';
import { CSS } from '@dnd-kit/utilities';
import { useSortable } from '@dnd-kit/sortable';
import styled from 'styled-components';

import type { EntityBase, ColumnMetaContext } from 'components/common/EntityDataTable/types';

const Td = styled.td`
  word-break: break-word;
`;

const TableCell = <Entity extends EntityBase>({ cell }: { cell: Cell<Entity, unknown> }) => {
  const columnMeta = cell.column.columnDef.meta as ColumnMetaContext<Entity>;

  const { isDragging, setNodeRef, transform } = useSortable({
    id: cell.column.id,
    disabled: !columnMeta?.enableColumnOrdering,
  });

  const style: CSSProperties = {
    opacity: isDragging ? 0.8 : 1,
    position: 'relative',
    transform: CSS.Translate.toString(transform), // translate instead of transform to avoid squishing
    transition: 'width transform 0.2s ease-in-out',
    width: cell.column.getSize(),
    zIndex: isDragging ? 1 : 0,
  };

  return (
    <Td ref={setNodeRef} style={style}>
      {flexRender(cell.column.columnDef.cell, cell.getContext())}
    </Td>
  );
};

export default TableCell;
