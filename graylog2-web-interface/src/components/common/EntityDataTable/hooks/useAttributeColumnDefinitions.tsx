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
import { useCallback, useMemo, useContext, useLayoutEffect } from 'react';
import type { createColumnHelper, Row, Column, HeaderContext, CellContext } from '@tanstack/react-table';
import camelCase from 'lodash/camelCase';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { styled, css } from 'styled-components';

import type {
  EntityBase,
  ColumnRenderersByAttribute,
  ColumnMetaContext,
} from 'components/common/EntityDataTable/types';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';
import { DRAG_HANDLE_DEFAULT_TITLE } from 'components/common/SortableList/DragHandle';
import DndStylesContext from 'components/common/EntityDataTable/contexts/DndStylesContext';
import IsResizingColumnContext from 'components/common/EntityDataTable/contexts/IsResizingColumnContext';
import useHeaderSectionObserver from 'components/common/EntityDataTable/hooks/useHeaderSectionObserver';
import HeaderActionsDropdown from 'components/common/EntityDataTable/HeaderActionsDropdown';
import Icon from 'components/common/Icon';
import ActiveSliceColContext from 'components/common/EntityDataTable/contexts/ActiveSliceColContext';

import SortIcon from '../SortIcon';

export const DragIcon = styled(Icon)<{ $isDragging?: boolean }>(
  ({ theme, $isDragging }) => css`
    position: absolute;
    top: -3px;
    left: 50%;
    transform: translateX(-50%) rotate(90deg);
    color: ${theme.colors.text.secondary};
    opacity: ${$isDragging ? 1 : 0};
    transition: opacity 0.15s ease-in-out;
    pointer-events: none;
    font-size: 12px;
  `,
);

export const ThInner = styled.div<{ $isDraggable?: boolean; $isDragging?: boolean }>(
  ({ theme, $isDraggable, $isDragging }) => css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    height: 100%;
    position: relative;
    // th no longer has its own padding, so this is the only padding for the header cell,
    // meaning it's part of this element's own box and therefore draggable/hoverable too.
    // Top/bottom padding is +2.5px each (+5px total) to make the header row a bit taller.
    padding: ${CELL_PADDING + 2.5}px ${CELL_PADDING}px;
    //padding-top: ${$isDraggable ? `calc(${CELL_PADDING}px + ${theme.spacings.xxs})` : `${CELL_PADDING}px`};

    ${$isDraggable &&
    css`
      cursor: ${$isDragging ? 'grabbing' : 'grab'};

      &:hover ${DragIcon} {
        opacity: 1;
      }
    `}
  `,
);

export const LeftCol = styled.div`
  display: flex;
  align-items: center;
  height: 100%;
`;

// Sits in the same spot the old resize handle icon used to occupy.
export const RightCol = styled.div`
  display: flex;
  align-items: center;
  height: 100%;
`;

const ActiveSliceIcon = styled(Icon)(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xs};
    color: ${theme.colors.gray[20]};
  `,
);

const useSortableCol = (colId: string, disabled: boolean) => {
  const { setColumnTransform } = useContext(DndStylesContext);
  const { attributes, isDragging, listeners, setNodeRef, transform } = useSortable({
    id: colId,
    disabled,
  });
  const cssTransform = CSS.Translate.toString(transform);

  useLayoutEffect(() => {
    setColumnTransform((cur) => {
      if (cur[colId] === cssTransform) return cur;

      return { ...cur, [colId]: cssTransform };
    });
  }, [colId, setColumnTransform, cssTransform]);

  return {
    attributes,
    isDragging,
    listeners,
    setNodeRef,
  };
};

const AttributeHeader = <Entity extends EntityBase>({
  ctx,
  onHeaderSectionResize,
  onChangeSlicing,
  appSection,
}: {
  ctx: HeaderContext<Entity, unknown>;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  appSection: string;
}) => {
  const activeSliceCol = useContext(ActiveSliceColContext);
  const { isResizingColumn } = useContext(IsResizingColumnContext) ?? {};
  const colId = ctx.header.column.id;
  const columnMeta = ctx.column.columnDef.meta as ColumnMetaContext<Entity>;
  const isDraggable = Boolean(columnMeta?.enableColumnOrdering);
  // Suppress the drag affordance (cursor + hover indicator) while a column is being resized, so it
  // doesn't flicker on/off as the pointer passes over other headers during the resize drag.
  const showDragAffordance = isDraggable && !isResizingColumn;
  const { attributes, isDragging, listeners, setNodeRef } = useSortableCol(colId, !isDraggable);
  const leftRef = useHeaderSectionObserver(colId, 'left', onHeaderSectionResize);
  const columnLabel = columnMeta?.label ?? colId;
  const canSlice = columnMeta?.enableSlicing;
  const isSliceActive = activeSliceCol === colId;
  const canSort = ctx.header.column.getCanSort();
  const sortDirection = ctx.header.column.getIsSorted();
  const dragTitle =
    typeof columnLabel === 'string'
      ? `${DRAG_HANDLE_DEFAULT_TITLE} ${columnLabel.toLocaleLowerCase()}`
      : DRAG_HANDLE_DEFAULT_TITLE;

  return (
    <ThInner
      ref={setNodeRef}
      $isDraggable={showDragAffordance}
      $isDragging={isDragging}
      title={isDraggable ? dragTitle : undefined}
      aria-label={isDraggable ? dragTitle : undefined}
      {...(isDraggable ? { ...attributes, ...listeners } : {})}>
      {isDraggable && <DragIcon name="drag_indicator" size="xs" $isDragging={isDragging} />}
      <LeftCol ref={leftRef}>
        <HeaderActionsDropdown
          label={columnLabel}
          activeSort={sortDirection}
          isSliceActive={isSliceActive}
          onChangeSlicing={canSlice ? onChangeSlicing : undefined}
          sliceColumnId={colId}
          appSection={appSection}
          onSort={canSort ? (desc) => ctx.table.setSorting([{ id: colId, desc }]) : undefined}>
          {columnMeta?.columnRenderer?.renderHeader?.(columnLabel) ?? columnLabel}
        </HeaderActionsDropdown>
        {isSliceActive && <ActiveSliceIcon name="surgical" title={`Slicing by ${columnLabel}`} size="xs" />}
      </LeftCol>
      {sortDirection && (
        <RightCol>
          <SortIcon<Entity> column={ctx.header.column} />
        </RightCol>
      )}
    </ThInner>
  );
};

const useAttributeColumnDefinitions = <Entity extends EntityBase, Meta>({
  columnHelper,
  columnRenderersByAttribute,
  columnSchemas,
  columnWidths,
  entityAttributesAreCamelCase,
  enableSlicing,
  meta,
  onChangeSlicing,
  onHeaderSectionResize,
  appSection,
}: {
  columnHelper: ReturnType<typeof createColumnHelper<Entity>>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnSchemas: Array<ColumnSchema>;
  columnWidths: { [attributeId: string]: number };
  entityAttributesAreCamelCase: boolean;
  enableSlicing: boolean;
  meta: Meta;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
  appSection?: string;
}) => {
  const cell = useCallback(
    ({
      row,
      getValue,
      column,
    }: {
      row: Row<Entity>;
      getValue: CellContext<Entity, unknown>['getValue'];
      column: Column<Entity>;
    }) => {
      const columnDefMeta = column.columnDef.meta as ColumnMetaContext<Entity>;

      return columnDefMeta?.columnRenderer?.renderCell?.(getValue(), row.original, meta) ?? getValue();
    },
    [meta],
  );

  const header = useCallback(
    (ctx) => (
      <AttributeHeader<Entity>
        ctx={ctx}
        onHeaderSectionResize={onHeaderSectionResize}
        onChangeSlicing={onChangeSlicing}
        appSection={appSection}
      />
    ),
    [appSection, onChangeSlicing, onHeaderSectionResize],
  );

  return useMemo(
    () =>
      columnSchemas.map((col) => {
        const baseColDef = {
          id: col.id,
          cell,
          header,
          size: columnWidths[col.id],
          enableHiding: true,
          enableResizing: !columnRenderersByAttribute[col.id].staticWidth,
          meta: {
            label: col.title,
            enableSlicing: enableSlicing && col.sliceable,
            columnRenderer: columnRenderersByAttribute[col.id],
            enableColumnOrdering: true,
          },
        };

        if (col.isDerived) {
          return columnHelper.display(baseColDef);
        }

        const attributeName = entityAttributesAreCamelCase ? camelCase(col.id) : col.id;

        return columnHelper.accessor((row) => row[attributeName], {
          enableSorting: col.sortable ?? false,
          ...baseColDef,
        });
      }),
    [
      columnSchemas,
      cell,
      header,
      columnWidths,
      columnRenderersByAttribute,
      enableSlicing,
      entityAttributesAreCamelCase,
      columnHelper,
    ],
  );
};

export default useAttributeColumnDefinitions;
