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
import { useCallback, useContext, useMemo, useState } from 'react';
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
import IsResizingColumnContext from 'components/common/EntityDataTable/contexts/IsResizingColumnContext';
import { DropdownCaret } from 'components/common/EntityDataTable/HeaderActionsDropdown';

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
  $zIndex: number;
}>(
  ({ $colId, $hidePadding, $pinningPosition, $zIndex, theme }) => css`
    position: relative;
    // Earlier columns win the shared border with their right neighbor, so a resize hit area
    // that overhangs into the next column's box still receives hover/pointer events there.
    // Values stay positive on purpose: a negative z-index on a th that also has a transform
    // (for column drag/reorder) can make some browsers cull the cell's own content entirely.
    padding: 0 !important;
    font-weight: normal; // override the browser default bold th styling
    // Use the row divider color for the border between headers, instead of the columnDivider
    // color the shared "columnBorders" table rule uses for the body rows.
    border-right-color: ${theme.colors.table.row.divider} !important;
    z-index: ${$pinningPosition ? 2000 : $zIndex};
    width: var(${columnWidthVar($colId)});
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, translate3d(0, 0, 0));
    transition: var(${columnTransition()}, none);
    height: 100%; // required to be able to use height: 100% in child elements

    &:hover ${DropdownCaret} {
      opacity: 1;
    }

    ${$pinningPosition
      ? css`
          position: sticky;
          ${$pinningPosition === 'left' ? 'left' : 'right'}: 0;
          background-color: ${theme.utils.flattenColorStack([
            theme.colors.global.contentBackground,
            theme.colors.table.head.background,
          ])};
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

// Tracks the resize gesture directly, rather than relying on react-table's columnSizingInfo state,
// so the highlight stays on for the whole drag regardless of state-update timing. Also flips the
// table-wide "is resizing" flag, so other headers can suppress their own hover affordances (e.g.
// the drag indicator) while the cursor passes over them during the resize drag.
const useIsResizingColumn = (setIsResizingColumnShared: (isResizingColumn: boolean) => void) => {
  const [isResizing, setIsResizing] = useState(false);

  const bindResizeHandler = useCallback(
    (handler: (event: unknown) => void) => (event: unknown) => {
      handler(event);
      setIsResizing(true);
      setIsResizingColumnShared(true);

      const stopResizing = () => {
        setIsResizing(false);
        setIsResizingColumnShared(false);
        window.removeEventListener('mouseup', stopResizing);
        window.removeEventListener('touchend', stopResizing);
      };

      window.addEventListener('mouseup', stopResizing);
      window.addEventListener('touchend', stopResizing);
    },
    [setIsResizingColumnShared],
  );

  return { isResizing, bindResizeHandler };
};

const TableHeaderCell = <Entity extends EntityBase>({ header }: { header: Header<Entity, unknown> }) => {
  const columnMeta = header.column.columnDef.meta as ColumnMetaContext<Entity>;
  const { setIsResizingColumn } = useContext(IsResizingColumnContext);
  const { isResizing, bindResizeHandler } = useIsResizingColumn(setIsResizingColumn);

  return (
    <Th
      key={header.id}
      colSpan={header.colSpan}
      $colId={header.column.id}
      $hidePadding={columnMeta?.hideCellPadding}
      $pinningPosition={header.column.getIsPinned()}
      $zIndex={1000 - header.index}>
      {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
      {header.column.getCanResize() && (
        <ResizeHitArea
          onMouseDown={bindResizeHandler(header.getResizeHandler())}
          onTouchStart={bindResizeHandler(header.getResizeHandler())}
          $isResizing={isResizing}
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

const TableHead = <Entity extends EntityBase>({ headerGroups }: Props<Entity>) => {
  const [isResizingColumn, setIsResizingColumn] = useState(false);
  const contextValue = useMemo(() => ({ isResizingColumn, setIsResizingColumn }), [isResizingColumn]);

  return (
    <IsResizingColumnContext.Provider value={contextValue}>
      <Thead>
        {headerGroups.map((headerGroup) => (
          <tr key={headerGroup.id}>
            {headerGroup.headers.map((header) => (
              <TableHeaderCell key={header.id} header={header} />
            ))}
          </tr>
        ))}
      </Thead>
    </IsResizingColumnContext.Provider>
  );
};

export default TableHead;
