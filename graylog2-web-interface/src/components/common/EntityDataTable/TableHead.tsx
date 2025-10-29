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
import { useMemo } from 'react';
import { flexRender } from '@tanstack/react-table';

import SortIcon from 'components/streams/StreamsOverview/SortIcon';
import type { Sort } from 'stores/PaginationTypes';

import BulkSelectHead from './BulkSelectHead';
import type { Column, ColumnRenderer, EntityBase, ColumnRenderersByAttribute } from './types';

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

const TableHeader = <Entity extends EntityBase>({
  header,
  activeSort,
  onSortChange,
}: {
  activeSort: Sort;
  onSortChange: (newSort: Sort) => void;
  header: any;
}) => (
  // const content = useMemo(
  //   () => (typeof columnRenderer?.renderHeader === 'function' ? columnRenderer.renderHeader(column) : column.title),
  //   [column, columnRenderer],
  // );

  <Th $width={header.getSize()} colSpan={header.colSpan}>
    {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
    {header.column.getCanSort() && <SortIcon onChange={onSortChange} header={header} activeSort={activeSort} />}
    {/*{header.column.getCanResize() && <div>Resize handle</div>}*/}
  </Th>
);

const ActionsHead = styled(Th)<{ $width: number | undefined }>(
  ({ $width }) => css`
    text-align: right;
    width: ${$width ? `${$width}px` : 'auto'};
  `,
);

const TableHead = <Entity extends EntityBase>({
  actionsColWidth,
  activeSort,
  columns,
  columnsOrder,
  columnRenderersByAttribute,
  data,
  displayActionsCol,
  displayBulkSelectCol,
  onSortChange,
  table,
}: {
  actionsColWidth: number | undefined;
  activeSort: Sort;
  columns: Array<Column>;
  columnsOrder: Array<string>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity>;
  data: Readonly<Array<Entity>>;
  displayActionsCol: boolean;
  displayBulkSelectCol: boolean;
  onSortChange: (newSort: Sort) => void;
  table: any;
}) => (
  // Todo can we include this in the tanstack table logic?
  // const sortedColumns = useMemo(
  //   () => columns.sort((col1, col2) => columnsOrder.indexOf(col1.id) - columnsOrder.indexOf(col2.id)),
  //   [columns, columnsOrder],
  // );

  <Thead>
    {table.getHeaderGroups().map((headerGroup) => (
      <tr key={headerGroup.id}>
        {/*{displayBulkSelectCol && <BulkSelectHead data={data} />}*/}
        {headerGroup.headers.map((header) => (
          <TableHeader<Entity> header={header} onSortChange={onSortChange} activeSort={activeSort} key={header.id} />
        ))}
        {displayActionsCol ? <ActionsHead $width={actionsColWidth}>Actions</ActionsHead> : null}
      </tr>
    ))}
  </Thead>
);
export default TableHead;
