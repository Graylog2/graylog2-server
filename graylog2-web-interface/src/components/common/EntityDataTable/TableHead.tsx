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
import type { Table, Header } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import type { CSSProperties } from 'react';

import SortIcon from 'components/common/EntityDataTable/SortIcon';
import Icon from 'components/common/Icon';

import type { EntityBase } from './types';

const Thead = styled.thead(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
  `,
);

export const Th = styled.th<{ $width: number | undefined }>(
  ({ $width, theme }) => css`
    width: ${$width ? `${$width}px` : 'auto'};
    background-color: ${theme.colors.table.head.background};
  `,
);

const DragHandle = styled.div<{ $isDragging: boolean }>(
  ({ $isDragging }) => css`
    display: inline-block;
    cursor: ${$isDragging ? 'grabbing' : 'grab'};
  `,
);

const DragIcon = styled(Icon)`
  color: ${({ theme }) => theme.colors.text.secondary};
`;

const TableHeaderCell = <Entity extends EntityBase>({ header }: { header: Header<Entity, unknown> }) => {
  const { attributes, isDragging, listeners, setNodeRef, transform } = useSortable({
    id: header.column.id,
    disabled: !header.column.columnDef.meta?.enableColumnOrdering,
  });

  const style: CSSProperties = {
    opacity: isDragging ? 0.8 : 1,
    position: 'relative',
    transform: CSS.Translate.toString(transform), // translate instead of transform to avoid squishing
    transition: 'width transform 0.2s ease-in-out',
    whiteSpace: 'nowrap',
    width: header.column.getSize(),
    zIndex: isDragging ? 1 : 0,
  };

  return (
    <Th $width={header.getSize()} colSpan={header.colSpan} key={header.id} ref={setNodeRef} style={style}>
      {header.column.columnDef.meta?.enableColumnOrdering && (
        <DragHandle {...attributes} {...listeners} $isDragging={isDragging}>
          <DragIcon name="drag_indicator" />
        </DragHandle>
      )}
      {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
      {header.column.getCanSort() && <SortIcon<Entity> header={header} />}
      {/*{header.column.getCanResize() && <div>Resize handle</div>}*/}
    </Th>
  );
};

const TableHead = <Entity extends EntityBase>({ table }: { table: Table<Entity> }) => (
  <Thead>
    {table.getHeaderGroups().map((headerGroup) => (
      <tr key={headerGroup.id}>
        {headerGroup.headers.map((header) => (
          <TableHeaderCell key={header.id} header={header} table={table} />
        ))}
      </tr>
    ))}
  </Thead>
);
export default TableHead;
