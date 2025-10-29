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
import type { Cell } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';

import type { EntityBase } from './types';

const Td = styled.td`
  word-break: break-word;
`;

const TableCell = <Entity extends EntityBase, Meta>({ cell }: { cell: Cell<Entity, undefined> }) => (
  // const { meta } = useMetaDataContext<Meta>();
  // const attributeValue = entity[attributeKey];
  // const content =
  //   typeof columnRenderer?.renderCell === 'function'
  //     ? columnRenderer.renderCell(attributeValue, entity, column, meta)
  //     : attributeValue;

  <Td>{flexRender(cell.column.columnDef.cell, cell.getContext())}</Td>
);
export default TableCell;
