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
import { arrayMove, horizontalListSortingStrategy, SortableContext } from '@dnd-kit/sortable';
import {
  closestCenter,
  DndContext,
  useSensors,
  useSensor,
  MouseSensor,
  TouchSensor,
  KeyboardSensor,
  type DragStartEvent,
  type DragEndEvent,
  DragOverlay,
  pointerWithin,
} from '@dnd-kit/core';
import { snapCenterToCursor } from '@dnd-kit/modifiers';
import { useState, forwardRef, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import styled, { css } from 'styled-components';
import type { Table } from '@tanstack/react-table';

import zIndices from 'theme/z-indices';
import type { EntityBase, ColumnMetaContext } from 'components/common/EntityDataTable/types';
import { Icon } from 'components/common';
import { UTILITY_COLUMNS } from 'components/common/EntityDataTable/Constants';

const CustomDragOverlay = styled.div(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
    z-index: ${zIndices.dropdownMenu};
    padding: ${theme.spacings.sm};
    width: min-content;
    font-weight: bold;
    white-space: nowrap;
    box-shadow: ${theme.colors.input.boxShadow};
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
  `,
);

const Item = forwardRef(({ title }: { title: string }, ref: React.ForwardedRef<HTMLDivElement>) => (
  <CustomDragOverlay ref={ref}>
    <Icon name="drag_pan" /> {title}
  </CustomDragOverlay>
));

const activeColumnTitle = <Entity extends EntityBase>(table: Table<Entity>, activeId: string | number) => {
  const activeColumnMeta = table.getAllColumns().find((col) => col.id === activeId)?.columnDef
    ?.meta as ColumnMetaContext<Entity>;

  return activeColumnMeta.label;
};

type Props<Entity extends EntityBase> = React.PropsWithChildren<{
  columnOrder: Array<string>;
  onColumnOrderChange: any;
  table: Table<Entity>;
}>;

const TableDndProvider = <Entity extends EntityBase>({
  columnOrder,
  onColumnOrderChange,
  children = undefined,
  table,
}: Props<Entity>) => {
  const [activeId, setActiveId] = useState<number | string | null>(null);

  const sensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));
  const handleDragStart = useCallback((event: DragStartEvent) => {
    const { active } = event;

    setActiveId(active.id);
  }, []);

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      const { active, over } = event;
      if (active && over && active.id !== over.id) {
        onColumnOrderChange((curColumnOrder) => {
          const oldIndex = curColumnOrder.indexOf(active.id as string);
          const newIndex = curColumnOrder.indexOf(over.id as string);

          return arrayMove(curColumnOrder, oldIndex, newIndex); //this is just a splice util
        });
      }

      setActiveId(null);
    },
    [onColumnOrderChange],
  );

  const draggableColumns = useMemo(() => columnOrder.filter((id) => !UTILITY_COLUMNS.has(id)), [columnOrder]);

  return (
    <DndContext
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
      modifiers={[snapCenterToCursor]}
      onDragStart={handleDragStart}
      sensors={sensors}>
      <SortableContext items={draggableColumns} strategy={horizontalListSortingStrategy}>
        {children}
      </SortableContext>
      <DragOverlay dropAnimation={null}>
        {activeId ? <Item title={activeColumnTitle(table, activeId)} /> : null}
      </DragOverlay>
    </DndContext>
  );
};

export default TableDndProvider;
