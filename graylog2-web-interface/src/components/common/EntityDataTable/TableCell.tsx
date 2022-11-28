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
import { useMemo } from 'react';
import styled, { css } from 'styled-components';

import type { Column, ColumnRenderer } from './types';

const Td = styled.td<{ $width: number | undefined, $maxWidth: string| undefined }>(({ $width, $maxWidth }) => css`
  width: ${$width ? `${$width}px` : 'auto'};
  max-width: ${$maxWidth ?? 'none'};
`);

const TableCell = <Entity extends { id: string }>({
  column,
  columnRenderer,
  entity,
  colWidth,
}: {
  column: Column
  columnRenderer: ColumnRenderer<Entity> | undefined,
  entity: Entity,
  colWidth: number
}) => {
  const content = useMemo(
    () => (typeof columnRenderer?.renderCell === 'function' ? columnRenderer.renderCell(entity, column) : entity[column.id]),
    [column, columnRenderer, entity],
  );

  return (
    <Td $width={colWidth} $maxWidth={columnRenderer?.maxWidth}>
      {content}
    </Td>
  );
};

export default TableCell;
