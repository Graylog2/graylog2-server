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
import { useCallback, useMemo, useContext, useLayoutEffect, useRef, useState } from 'react';
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
import useMergedRef from 'util/hooks/useMergedRef';

import SortIcon from '../SortIcon';

export const DragIcon = styled(Icon)<{ $isDragging?: boolean }>(
  ({ theme, $isDragging }) => css`
    position: absolute;
    top: 0px;
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
  ({ $isDraggable, $isDragging }) => css`
    display: flex;
    // Always space-between: for right-aligned (numeric) columns, the sort icon and title group
    // are simply reordered (see AttributeHeader) so this spreads them mirrored, sort-icon-then-title
    // instead of title-then-sort-icon, rather than clustering them together.
    justify-content: space-between;
    align-items: center;
    width: 100%;
    height: 100%;
    position: relative;
    // th no longer has its own padding, so this is the only padding for the header cell,
    // meaning it's part of this element's own box and therefore draggable/hoverable too.
    // Top/bottom padding is +2.5px each (+5px total) to make the header row a bit taller.
    padding: ${CELL_PADDING_HEADER};

    ${$isDraggable &&
    css`
      // "pointer", not "grab": a plain click opens the actions menu, which is the more common
      // interaction, so the cursor hints at that instead of the (still available) drag-to-reorder.
      cursor: ${$isDragging ? 'grabbing' : 'pointer'};

      &:hover ${DragIcon}, &:focus-visible ${DragIcon} {
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
  // const { isResizingColumn } = useContext(IsResizingColumnContext) ?? {};
  const colId = ctx.header.column.id;
  const columnMeta = ctx.column.columnDef.meta as ColumnMetaContext<Entity>;
  const isDraggable = Boolean(columnMeta?.enableColumnOrdering);
  // Suppress the drag affordance (cursor + hover indicator) while a column is being resized, so it
  // doesn't flicker on/off as the pointer passes over other headers during the resize drag.
  // const showDragAffordance = isDraggable && !isResizingColumn;
  const showDragAffordance = isDraggable;
  const { attributes, isDragging, listeners, setNodeRef } = useSortableCol(colId, !isDraggable);
  const headerRef = useRef<HTMLDivElement>(null);
  const mergedHeaderRef = useMergedRef(setNodeRef, headerRef);
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
  const hasHeaderActions = Boolean(canSort || canSlice || canHideColumn);
  const [isActionsMenuOpen, setIsActionsMenuOpen] = useState(false);
  const [menuAnchorPosition, setMenuAnchorPosition] = useState<{ x: number; y: number } | null>(null);

  // Mantine's own dropdowns return focus to the button that opened them once they close; its
  // built-in mechanism for that (Popover's `returnFocus`) just remembers `document.activeElement`
  // at open time and refocuses it at close time, independent of the target/anchor element -- but it
  // isn't landing back on the header reliably for us here (likely something about the anchor being
  // a portaled, non-focusable virtual point rather than the actual visible button Mantine expects).
  // Doing it ourselves is simple and matches that same "usual dropdown" behavior: closing the menu,
  // for any reason (an action, Escape, an outside click, or clicking the header again), always
  // returns focus to the header cell itself.
  const onActionsMenuOpenChange = (nextOpen: boolean) => {
    setIsActionsMenuOpen(nextOpen);

    if (!nextOpen) {
      headerRef.current?.focus({ preventScroll: true });
    }
  };

  const openActionsMenuAt = (position: { x: number; y: number }) => {
    if (isActionsMenuOpen) {
      onActionsMenuOpenChange(false);

      return;
    }

    setMenuAnchorPosition(position);
    setIsActionsMenuOpen(true);
  };

  const onHeaderClick = (event: React.MouseEvent<HTMLDivElement>) => {
    // A keyboard-triggered click (Enter/Space on a focused native button) reports (0, 0) here, so
    // fall back to anchoring at the header's own position instead of the top-left of the viewport.
    const { clientX, clientY } = event;
    const hasPointerPosition = clientX !== 0 || clientY !== 0;
    const rect = event.currentTarget.getBoundingClientRect();

    openActionsMenuAt(hasPointerPosition ? { x: clientX, y: clientY } : { x: rect.left, y: rect.bottom });
  };

  // Space is reserved for picking up the column drag (dnd-kit's keyboard sensor is configured to
  // only start on Space, see TableDndProvider), so Enter opens the actions menu instead -- matching
  // every other Mantine dropdown button in the app (focus it, press Enter). Only relevant before a
  // drag is picked up: once dragging, arrow keys move the column and Space/Tab drop it in place.
  const onHeaderKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    listeners?.onKeyDown?.(event);

    // Only for a keydown on the header itself, not one that bubbled up from a focused descendant
    // (the sort icon, or -- since Menu.Dropdown is rendered through a portal into <body>, React's
    // synthetic events still bubble along the *React* tree, not the DOM tree -- a focused menu
    // item). Otherwise Enter on either of those would open/close this menu instead of letting the
    // sort icon's or menu item's own onClick run.
    if (event.target !== event.currentTarget || isDragging || !hasHeaderActions || event.key !== 'Enter') {
      return;
    }

    event.preventDefault();
    const rect = event.currentTarget.getBoundingClientRect();
    openActionsMenuAt({ x: rect.left, y: rect.bottom });
  };

  // Always rendered (even with no active sort), so ThInner always has two flex children:
  // with only one child, "justify-content: space-between" collapses to "flex-start", pinning
  // a lone title group to the left even when this column is right-aligned.
  const sortIcon = (
    <RightCol ref={rightRef}>{sortDirection && <SortIcon<Entity> column={ctx.header.column} />}</RightCol>
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
        onSort={canSort ? (desc) => ctx.table.setSorting([{ id: colId, desc }]) : undefined}
        onHideColumn={canHideColumn ? () => ctx.header.column.toggleVisibility() : undefined}
        opened={isActionsMenuOpen}
        onOpenChange={onActionsMenuOpenChange}
        anchorPosition={menuAnchorPosition}>
        {columnMeta?.columnRenderer?.renderHeader?.(columnLabel) ?? columnLabel}
      </HeaderActionsDropdown>
      {isSliceActive && <ActiveSliceIcon name="surgical" title={`Slicing by ${columnLabel}`} size="xs" />}
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
      $isDraggable={showDragAffordance}
      $isDragging={isDragging}
      title={isDraggable ? dragTitle : undefined}
      aria-label={isDraggable ? dragTitle : undefined}
      {...(isDraggable ? { ...attributes, ...listeners } : {})}
      {...nonDraggableA11yProps}
      // Opens the actions menu on a click anywhere in the header, not just on the title itself, and
      // toggles it closed on a second click. Elements with their own dedicated click behavior (the
      // sort icon) stop propagation so this doesn't also react to a click they already handled.
      onClick={hasHeaderActions ? onHeaderClick : undefined}
      // Overrides the onKeyDown that `listeners` above may have set, so dnd-kit's own Space/Enter
      // drag-pickup handling still runs (see onHeaderKeyDown, which calls it explicitly) alongside
      // our ArrowDown-opens-the-menu behavior.
      onKeyDown={isDraggable || hasHeaderActions ? onHeaderKeyDown : undefined}>
      {isDraggable && <DragIcon name="drag_indicator" size="xs" $isDragging={isDragging} />}
      {isRightAligned ? (
        <>
          {sortIcon}
          {titleGroup}
        </>
      ) : (
        <>
          {titleGroup}
          {sortIcon}
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
