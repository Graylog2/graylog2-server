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
import useClickToOpenMenu from 'components/bootstrap/useClickToOpenMenu';
import Icon from 'components/common/Icon';
import ActiveSliceColContext from 'components/common/EntityDataTable/contexts/ActiveSliceColContext';
import useMergedRef from 'util/hooks/useMergedRef';

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

    /* pointer, not grab, below: a plain click opens the actions menu, which is the more common
       interaction, so the cursor hints at that instead of the (still available) drag-to-reorder. */
    ${$isDraggable &&
    css`
      cursor: ${$isDragging ? 'grabbing' : 'pointer'};

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
  const { triggerRef, opened, onOpenChange, anchorPosition, onClick, onKeyDown } = useClickToOpenMenu<HTMLDivElement>();
  const mergedHeaderRef = useMergedRef(setNodeRef, triggerRef);
  const leftRef = useHeaderSectionObserver(colId, 'left', onHeaderSectionResize);
  const rightRef = useHeaderSectionObserver(colId, 'right', onHeaderSectionResize);
  const columnLabel = columnMeta?.label ?? colId;
  const canSlice = columnMeta?.enableSlicing;
  const isSliceActive = activeSliceCol === colId;
  const canSort = ctx.header.column.getCanSort();
  const canHideColumn = ctx.header.column.getCanHide();
  const sortDirection = ctx.header.column.getIsSorted();
  const textAlign = columnMeta?.columnRenderer?.textAlign as 'left' | 'right';
  const isRightAligned = textAlign === 'right';
  const dragTitle =
    typeof columnLabel === 'string'
      ? `${DRAG_HANDLE_DEFAULT_TITLE} ${columnLabel.toLocaleLowerCase()}`
      : DRAG_HANDLE_DEFAULT_TITLE;
  const hasHeaderActions = Boolean(canSort || canSlice || canHideColumn);

  // Space is reserved for picking up the column drag (dnd-kit's keyboard sensor is configured to
  // only start on Space, see TableDndProvider), so Enter opens the actions menu instead -- matching
  // every other Mantine dropdown button in the app (focus it, press Enter). Only relevant before a
  // drag is picked up: once dragging, arrow keys move the column and Space/Tab drop it in place.
  const onHeaderKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    listeners?.onKeyDown?.(event);

    if (isDragging || !hasHeaderActions) {
      return;
    }

    onKeyDown(event);
  };

  const sliceIndicator = isSliceActive && (
    <ActiveSliceIcon name="surgical" title={`Slicing by ${columnLabel}`} size="xs" $isRightAligned={isRightAligned} />
  );
  const sortIndicator = sortDirection && <SortIcon<Entity> column={ctx.header.column} />;

  const indicatorIcons = (
    // Stops the sort icon's own click from also bubbling up to the header's onClick (which would
    // toggle the actions menu open/closed right after the sort click already did its own thing).
    <IndicatorCol ref={rightRef} onClick={(event) => event.stopPropagation()}>
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
        textAlign={textAlign}
        label={columnLabel}
        activeSort={sortDirection}
        isSliceActive={isSliceActive}
        onChangeSlicing={canSlice ? onChangeSlicing : undefined}
        sliceColumnId={colId}
        appSection={appSection}
        onSort={canSort ? (desc) => ctx.table.setSorting([{ id: colId, desc }]) : undefined}
        onHideColumn={canHideColumn ? () => ctx.header.column.toggleVisibility() : undefined}
        opened={opened}
        onOpenChange={onOpenChange}
        anchorPosition={anchorPosition}>
        {columnMeta?.columnRenderer?.renderHeader?.(columnLabel) ?? columnLabel}
      </HeaderActionsDropdown>
    </LeftCol>
  );

  // dnd-kit's own `attributes` already provide role="button"/tabIndex=0 when the column is
  // draggable; when it isn't, supply the same thing ourselves whenever there's a menu to reach --
  // this is the header's one focus stop (DropdownTrigger, in HeaderActionsDropdown, deliberately
  // isn't focusable, so Tab doesn't land on the header twice).
  const nonDraggableA11yProps = !isDraggable && hasHeaderActions ? { role: 'button' as const, tabIndex: 0 } : {};

  return (
    <ThInner
      ref={mergedHeaderRef}
      $isDraggable={isDraggable}
      $isDragging={isDragging}
      $isResizingAnyColumn={isResizingAnyColumn}
      title={isDraggable ? dragTitle : undefined}
      aria-label={isDraggable ? dragTitle : undefined}
      {...(isDraggable ? { ...attributes, ...listeners } : {})}
      {...nonDraggableA11yProps}
      // Opens the actions menu on a click anywhere in the header, not just on the title itself, and
      // toggles it closed on a second click. Elements with their own dedicated click behavior (the
      // sort icon) stop propagation so this doesn't also react to a click they already handled.
      onClick={hasHeaderActions ? onClick : undefined}
      // Overrides the onKeyDown that `listeners` above may have set, so dnd-kit's own Space/Enter
      // drag-pickup handling still runs (see onHeaderKeyDown, which calls it explicitly) alongside
      // our Enter-opens-the-menu behavior.
      onKeyDown={isDraggable || hasHeaderActions ? onHeaderKeyDown : undefined}>
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
