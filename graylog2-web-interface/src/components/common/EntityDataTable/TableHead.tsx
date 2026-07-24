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
import { useCallback } from 'react';
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
import useForceUpdate from 'util/hooks/useForceUpdate';

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
    position: relative;
    padding: 0 !important;
    z-index: 1;
    font-weight: normal; // override the browser default bold th styling
    border-right-color: ${theme.colors.table.row.divider} !important;
    width: var(${columnWidthVar($colId)});
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, translate3d(0, 0, 0));
    transition: var(${columnTransition()}, none);
    height: 100%; // required to be able to use height: 100% in child elements

    ${$pinningPosition
      ? css`
          position: sticky;
          ${$pinningPosition === 'left' ? 'left' : 'right'}: 0;
          background-color: ${theme.utils.flattenColorStack([
            theme.colors.global.contentBackground,
            theme.colors.table.head.background,
          ])};
          ${$pinningPosition === 'right' &&
          css`
            box-shadow: inset -1px 0 0 ${theme.colors.table.row.divider};
          `}
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

const ResizeHitArea = styled.div<{ $isResizing: boolean }>(
  ({ theme, $isResizing }) => css`
    position: absolute;
    top: 0;
    bottom: 0;
    right: -7px;
    width: 12px;
    cursor: col-resize;
    touch-action: none;
    user-select: none;
    z-index: 1;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      bottom: 0;
      left: 50%;
      width: 3px;
      transform: translateX(-50%);
      background-color: ${theme.colors.variant.info};
      opacity: ${$isResizing ? 1 : 0};
      transition: opacity 0.15s ease-in-out;
    }

    &:hover::after {
      opacity: 1;
    }
  `,
);

const TableHeaderCell = <Entity extends EntityBase>({ header }: { header: Header<Entity, unknown> }) => {
  const columnMeta = header.column.columnDef.meta as ColumnMetaContext<Entity>;
  const forceUpdate = useForceUpdate();

  // header.column.getIsResizing() is the source of truth, but nothing here re-renders on its own
  // when it changes: Table is wrapped in React.memo, and its headerGroups/rows props don't change
  // just because a column is being resized. Forcing a local re-render on resize start/end (rather
  // than tracking "is resizing" as its own bit of state) keeps this cell in sync without duplicating
  // what react-table already knows.
  const bindResizeHandler = useCallback(
    (handler: (event: unknown) => void) => (event: unknown) => {
      handler(event);
      forceUpdate();

      const stopResizing = () => {
        forceUpdate();
        window.removeEventListener('mouseup', stopResizing);
        window.removeEventListener('touchend', stopResizing);
      };

      window.addEventListener('mouseup', stopResizing);
      window.addEventListener('touchend', stopResizing);
    },
    [forceUpdate],
  );

  return (
    <Th
      key={header.id}
      colSpan={header.colSpan}
      $colId={header.column.id}
      $hidePadding={columnMeta?.hideCellPadding}
      $pinningPosition={header.column.getIsPinned()}>
      {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
      {header.column.getCanResize() && (
        <ResizeHitArea
          onMouseDown={bindResizeHandler(header.getResizeHandler())}
          onTouchStart={bindResizeHandler(header.getResizeHandler())}
          $isResizing={header.column.getIsResizing()}
          role="separator"
          aria-label={`Resize ${columnMeta?.label ?? header.column.id} column`}
          title={`Resize ${columnMeta?.label ?? header.column.id} column`}
        />
      )}
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
