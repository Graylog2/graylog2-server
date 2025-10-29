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

import SortIcon from 'components/common/EntityDataTable/SortIcon';
import type { Column, EntityBase } from './types';

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

const TableHeader = <Entity extends EntityBase>({ header }: { header: Header<Entity> }) => (
  <Th $width={header.getSize()} colSpan={header.colSpan}>
    {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
    {header.column.getCanSort() && <SortIcon header={header} />}
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
  displayActionsCol,
  table,
}: {
  actionsColWidth: number | undefined;
  columns: Array<Column>;
  columnsOrder: Array<string>;
  displayActionsCol: boolean;
  table: Table<Entity>;
}) => (
  <Thead>
    {table.getHeaderGroups().map((headerGroup) => (
      <tr key={headerGroup.id}>
        {headerGroup.headers.map((header) => (
          <TableHeader<Entity> header={header} key={header.id} />
        ))}
        {displayActionsCol ? <ActionsHead $width={actionsColWidth}>Actions</ActionsHead> : null}
      </tr>
    ))}
  </Thead>
);
export default TableHead;
