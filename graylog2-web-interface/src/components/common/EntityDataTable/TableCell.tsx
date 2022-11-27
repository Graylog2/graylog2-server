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

import type { Attribute } from './types';

const Td = styled.td<{ $width: string, $maxWidth: string }>(({ $width, $maxWidth }) => css`
  width: ${$width ?? 'auto'};
  max-width: ${$maxWidth ?? 'none'};
`);

const TableCell = <ListItem extends { id: string }>({
  attribute,
  cellRenderer,
  listItem,
}: {
  attribute: Attribute
  cellRenderer: {
    renderCell: (listItem: ListItem, attribute: Attribute) => React.ReactNode,
    width?: string,
    maxWidth?: string,
  },
  listItem: ListItem,
}) => {
  const content = useMemo(
    () => (cellRenderer ? cellRenderer.renderCell(listItem, attribute) : listItem[attribute.id]),
    [attribute, cellRenderer, listItem],
  );

  return (
    <Td $width={cellRenderer?.width} $maxWidth={cellRenderer?.maxWidth}>
      {content}
    </Td>
  );
};

export default TableCell;
