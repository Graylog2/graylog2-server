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
import type { Header, HeaderGroup, ColumnPinningPosition } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';

import {
  columnTransformVar,
  columnOpacityVar,
  columnWidthVar,
  columnTransition,
  displayScrollRightIndicatorVar,
} from 'components/common/EntityDataTable/CSSVariables';
import { ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';
import ScrollShadow from 'theme/box-shadows/ScrollShadow';

import type { EntityBase, ColumnMetaContext } from './types';

const Thead = styled.thead(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
  `,
);

export const Th = styled.th<{
  $colId: string;
  $hidePadding: boolean;
  $pinningPosition: ColumnPinningPosition;
}>(
  ({ $colId, $hidePadding, $pinningPosition, theme }) => css`
    width: var(${columnWidthVar($colId)});
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, translate3d(0, 0, 0));
    background-color: ${theme.colors.table.head.background};
    transition: var(${columnTransition()}, none);
    height: 100%; // required to be able to use height: 100% in child elements
    ${$pinningPosition
      ? css`
          position: sticky;
          ${$pinningPosition === 'left' ? 'left' : 'right'}: 0;
        `
      : ''}

    ${$hidePadding &&
    css`
      && {
        padding: 0;
      }
    `}

    ${$colId === ACTIONS_COL_ID &&
    css`
      position: sticky;
      ${ScrollShadow('left')}
      &::before {
        display: var(${displayScrollRightIndicatorVar}, none);
      }
    `}
  `,
);

const TableHeaderCell = <Entity extends EntityBase>({ header }: { header: Header<Entity, unknown> }) => {
  const columnMeta = header.column.columnDef.meta as ColumnMetaContext<Entity>;

  return (
    <Th
      key={header.id}
      colSpan={header.colSpan}
      $colId={header.column.id}
      $hidePadding={columnMeta?.hideCellPadding}
      $pinningPosition={header.column.getIsPinned()}>
      {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
    </Th>
  );
};

type Props<Entity extends EntityBase> = {
  headerGroups: Array<HeaderGroup<Entity>>;
};

const TableHead = <Entity extends EntityBase>({ headerGroups }: Props<Entity>) => (
  <Thead>
    {headerGroups.map((headerGroup) => (
      <tr key={headerGroup.id}>
        {headerGroup.headers.map((header) => (
          <TableHeaderCell key={header.id} header={header} />
        ))}
      </tr>
    ))}
  </Thead>
);

export default TableHead;
