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
import { CELL_PADDING_HEADER } from 'components/common/EntityDataTable/Constants';
import { DRAG_HANDLE_DEFAULT_TITLE } from 'components/common/SortableList/DragHandle';
import DndStylesContext from 'components/common/EntityDataTable/contexts/DndStylesContext';
import useHeaderSectionObserver from 'components/common/EntityDataTable/hooks/useHeaderSectionObserver';
import HeaderActionsDropdown from 'components/common/EntityDataTable/HeaderActionsDropdown';
import Icon from 'components/common/Icon';
import ActiveSliceColContext from 'components/common/EntityDataTable/contexts/ActiveSliceColContext';

import SortIcon from '../SortIcon';

export const DragIcon = styled(Icon)<{ $isDragging?: boolean }>(
  ({ theme, $isDragging }) => css`
    position: absolute;
    top: 0;
    left: 50%;
    transform: translateX(-50%) rotate(90deg);
    color: ${theme.colors.text.secondary};
    opacity: ${$isDragging ? 1 : 0};
    transition: opacity 0.15s ease-in-out;
    pointer-events: none;
    font-size: 12px;
  `,
);

export const ThInner = styled.div<{
  $isDraggable?: boolean;
  $isDragging?: boolean;
  $isResizingAnyColumn?: boolean;
}>(
  ({ $isDraggable, $isDragging, $isResizingAnyColumn }) => css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    height: 100%;
    position: relative;
    padding: ${CELL_PADDING_HEADER};

    ${!$isResizingAnyColumn &&
    css`
      &:hover .header-action {
        opacity: 1;
      }
    `}

    ${$isDraggable &&
    css`
      cursor: ${$isDragging ? 'grabbing' : 'grab'};

      &:focus-visible .header-action {
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

export const IndicatorCol = styled.div`
  display: flex;
  align-items: center;
  height: 100%;
`;

const ActiveSliceIcon = styled(Icon)<{ $isRightAligned?: boolean }>(
  ({ theme, $isRightAligned }) => css`
    ${$isRightAligned
      ? css`
          margin-right: ${theme.spacings.xs};
        `
      : css`
          margin-left: ${theme.spacings.xs};
        `}
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
  const colId = ctx.header.column.id;
  const columnMeta = ctx.column.columnDef.meta as ColumnMetaContext<Entity>;
  const isDraggable = Boolean(columnMeta?.enableColumnOrdering);
  const isResizingAnyColumn = Boolean(ctx.table.getState().columnSizingInfo.isResizingColumn);
  const { attributes, isDragging, listeners, setNodeRef } = useSortableCol(colId, !isDraggable);
  const leftRef = useHeaderSectionObserver(colId, 'left', onHeaderSectionResize);
  const rightRef = useHeaderSectionObserver(colId, 'right', onHeaderSectionResize);
  const columnLabel = columnMeta?.label ?? colId;
  const canSlice = columnMeta?.enableSlicing;
  const isSliceActive = activeSliceCol === colId;
  const canSort = ctx.header.column.getCanSort();
  const canHideColumn = ctx.header.column.getCanHide();
  const sortDirection = ctx.header.column.getIsSorted();
  const textAlign = columnMeta?.columnRenderer?.textAlign;
  const isRightAligned = textAlign === 'right';
  const dragTitle =
    typeof columnLabel === 'string'
      ? `${DRAG_HANDLE_DEFAULT_TITLE} ${columnLabel.toLocaleLowerCase()}`
      : DRAG_HANDLE_DEFAULT_TITLE;

  const sliceIndicator = isSliceActive && (
    <ActiveSliceIcon
      name="surgical"
      title={`Slicing by ${columnLabel}`}
      size="xs"
      $isRightAligned={isRightAligned}
    />
  );
  const sortIndicator = sortDirection && <SortIcon<Entity> column={ctx.header.column} />;

  const indicatorIcons = (
    <IndicatorCol ref={rightRef}>
      {isRightAligned ? (
        <>
          {sortIndicator}
          {sliceIndicator}
        </>
      ) : (
        <>
          {sliceIndicator}
          {sortIndicator}
        </>
      )}
    </IndicatorCol>
  );

  const titleGroup = (
    <LeftCol ref={leftRef}>
      <HeaderActionsDropdown
        label={columnLabel}
        activeSort={sortDirection}
        isSliceActive={isSliceActive}
        onChangeSlicing={canSlice ? onChangeSlicing : undefined}
        sliceColumnId={colId}
        appSection={appSection}
        textAlign={textAlign}
        onSort={canSort ? (desc) => ctx.table.setSorting([{ id: colId, desc }]) : undefined}
        onHideColumn={canHideColumn ? () => ctx.header.column.toggleVisibility() : undefined}>
        {columnMeta?.columnRenderer?.renderHeader?.(columnLabel) ?? columnLabel}
      </HeaderActionsDropdown>
    </LeftCol>
  );

  return (
    <ThInner
      ref={setNodeRef}
      $isDraggable={isDraggable}
      $isDragging={isDragging}
      $isResizingAnyColumn={isResizingAnyColumn}
      title={isDraggable ? dragTitle : undefined}
      aria-label={isDraggable ? dragTitle : undefined}
      {...(isDraggable ? { ...attributes, ...listeners } : {})}>
      {isDraggable && <DragIcon name="drag_indicator" size="xs" $isDragging={isDragging} className="header-action" />}
      {isRightAligned ? (
        <>
          {indicatorIcons}
          {titleGroup}
        </>
      ) : (
        <>
          {titleGroup}
          {indicatorIcons}
        </>
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
          enableResizing: true,
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
