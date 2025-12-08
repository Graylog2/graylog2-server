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
import { useContext, useLayoutEffect } from 'react';
import styled, { css } from 'styled-components';
import type { Header, HeaderGroup, ColumnPinningPosition } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

import DragHandle from 'components/common/SortableList/DragHandle';
import {
  columnTransformVar,
  columnOpacityVar,
  columnWidthVar,
  columnTransition,
} from 'components/common/EntityDataTable/CSSVariables';
import { ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';

import SortIcon from './SortIcon';
import DndStylesContext from './contexts/DndStylesContext';
import ResizeHandle from './ResizeHandle';
import type { EntityBase, ColumnMetaContext } from './types';
import useHeaderSectionObserver from './hooks/useHeaderSectionObserver';

const Thead = styled.thead(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
  `,
);

export const Th = styled.th<{ $colId: string; $hidePadding: boolean; $pinningPosition: ColumnPinningPosition }>(
  ({ $colId, $hidePadding, $pinningPosition, theme }) => css`
    width: var(${columnWidthVar($colId)});
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, translate3d(0, 0, 0));
    background-color: ${theme.colors.table.head.background};
    transition: var(${columnTransition()}, none);
    height: 0;
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
  `,
);

export const ThInner = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
`;

export const LeftCol = styled.div`
  display: flex;
  align-items: center;
`;

const RightCol = styled.div`
  display: flex;
  align-items: center;
`;

const useSortableCol = (colId: string, disabled: boolean) => {
  const { setColumnTransform } = useContext(DndStylesContext);
  const { attributes, isDragging, listeners, setNodeRef, transform, setActivatorNodeRef } = useSortable({
    id: colId,
    disabled,
  });
  const cssTransform = CSS.Translate.toString(transform);

  useLayoutEffect(() => {
    setColumnTransform((cur) => ({
      ...cur,
      [colId]: cssTransform,
    }));
  }, [colId, setColumnTransform, cssTransform]);

  return {
    attributes,
    isDragging,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
  };
};

const TableHeaderCell = <Entity extends EntityBase>({
  header,
  hasRowActions,
  onHeaderSectionResize,
}: {
  header: Header<Entity, unknown>;
  hasRowActions: boolean;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
}) => {
  const columnMeta = header.column.columnDef.meta as ColumnMetaContext<Entity>;
  const { attributes, isDragging, listeners, setNodeRef, setActivatorNodeRef } = useSortableCol(
    header.column.id,
    !columnMeta?.enableColumnOrdering,
  );
  const leftRef = useHeaderSectionObserver(header.column.id, 'left', onHeaderSectionResize);
  const rightRef = useHeaderSectionObserver(header.column.id, 'right', onHeaderSectionResize);

  return (
    <Th
      key={header.id}
      ref={setNodeRef}
      colSpan={header.colSpan}
      $colId={header.column.id}
      $hidePadding={!hasRowActions && header.column.id === ACTIONS_COL_ID}
      $pinningPosition={header.column.getIsPinned()}>
      <ThInner>
        <LeftCol ref={leftRef}>
          {columnMeta?.enableColumnOrdering && (
            <DragHandle
              ref={setActivatorNodeRef}
              index={header.index}
              dragHandleProps={{ ...attributes, ...listeners }}
              isDragging={isDragging}
              itemTitle={columnMeta.label}
            />
          )}
          {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
          {header.column.getCanSort() && <SortIcon<Entity> column={header.column} />}
        </LeftCol>
        <RightCol ref={rightRef}>
          {header.column.getCanResize() && (
            <ResizeHandle
              onMouseDown={header.getResizeHandler()}
              onTouchStart={header.getResizeHandler()}
              colTitle={columnMeta.label}
            />
          )}
        </RightCol>
      </ThInner>
    </Th>
  );
};

type Props<Entity extends EntityBase> = {
  hasRowActions: boolean;
  headerGroups: Array<HeaderGroup<Entity>>;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
};

const TableHead = <Entity extends EntityBase>({
  headerGroups,
  hasRowActions,
  onHeaderSectionResize,
}: Props<Entity>) => (
  <Thead>
    {headerGroups.map((headerGroup) => (
      <tr key={headerGroup.id}>
        {headerGroup.headers.map((header) => (
          <TableHeaderCell
            key={header.id}
            header={header}
            hasRowActions={hasRowActions}
            onHeaderSectionResize={onHeaderSectionResize}
          />
        ))}
      </tr>
    ))}
  </Thead>
);

export default TableHead;
