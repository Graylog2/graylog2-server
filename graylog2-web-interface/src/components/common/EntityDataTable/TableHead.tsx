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
import type { Table } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';

import SortIcon from 'components/common/EntityDataTable/SortIcon';

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

const TableHead = <Entity extends EntityBase>({ table }: { table: Table<Entity> }) => (
  <Thead>
    {table.getHeaderGroups().map((headerGroup) => (
      <tr key={headerGroup.id}>
        {headerGroup.headers.map((header) => (
          <Th $width={header.getSize()} colSpan={header.colSpan} key={header.id}>
            {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
            {header.column.getCanSort() && <SortIcon header={header} />}
            {/*{header.column.getCanResize() && <div>Resize handle</div>}*/}
          </Th>
        ))}
      </tr>
    ))}
  </Thead>
);
export default TableHead;
