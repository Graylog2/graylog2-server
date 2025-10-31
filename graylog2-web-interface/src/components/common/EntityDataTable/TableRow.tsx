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
import type { Row } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import { styled } from 'styled-components';

import type { EntityBase } from 'components/common/EntityDataTable/types';

const Td = styled.td`
  word-break: break-word;
`;

type Props<Entity extends EntityBase> = {
  row: Row<Entity>;
};

const TableRow = <Entity extends EntityBase>({ row }: Props<Entity>) => (
  <tr>
    {row.getVisibleCells().map((cell) => (
      <Td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</Td>
    ))}
  </tr>
);

export default React.memo(TableRow) as typeof TableRow;
