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
import { useState, useCallback, useMemo } from 'react';
import { arrayMove, horizontalListSortingStrategy, SortableContext } from '@dnd-kit/sortable';
import type { DragStartEvent, DragEndEvent } from '@dnd-kit/core';
import {
  PointerSensor,
  DndContext,
  useSensors,
  useSensor,
  MouseSensor,
  TouchSensor,
  KeyboardSensor,
  DragOverlay,
} from '@dnd-kit/core';
import type { Table } from '@tanstack/react-table';
import { createPortal } from 'react-dom';

import type { EntityBase } from 'components/common/EntityDataTable/types';
import { UTILITY_COLUMNS } from 'components/common/EntityDataTable/Constants';
import ThDragOverlay from 'components/common/EntityDataTable/ThDragOverlay';
import DndStylesProvider from 'components/common/EntityDataTable/contexts/DndStylesProvider';
import zIndices from 'theme/z-indices';
import useDndCollisionDetection from 'components/common/EntityDataTable/hooks/useDndCollisionDetection';

type Props<Entity extends EntityBase> = React.PropsWithChildren<{
  table: Table<Entity>;
}>;

const TableDndProvider = <Entity extends EntityBase>({ children = undefined, table }: Props<Entity>) => {
  const [activeId, setActiveId] = useState<number | string | null>(null);
  const columnOrder = table.getState().columnOrder;
  const draggableColumns = useMemo(() => columnOrder.filter((id) => !UTILITY_COLUMNS.has(id)), [columnOrder]);
  const { collisionDetection, setLastOverId } = useDndCollisionDetection(draggableColumns);
  const activeColumn = useMemo(
    () => (activeId ? table.getAllColumns().find((col) => col.id === activeId) : undefined),
    [activeId, table],
  );

  const sensors = useSensors(
    useSensor(MouseSensor, {}),
    useSensor(TouchSensor, {}),
    useSensor(PointerSensor, {}),
    useSensor(KeyboardSensor, {}),
  );
  const handleDragStart = useCallback(
    ({ active }: DragStartEvent) => {
      setActiveId(active.id);
      setLastOverId(active.id);
    },
    [setLastOverId],
  );

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      const { active, over } = event;

      if (active && over?.id && active.id !== over.id) {
        table.setColumnOrder((curColumnOrder) => {
          const oldIndex = curColumnOrder.indexOf(active.id.toString());
          const newIndex = curColumnOrder.indexOf(over.id.toString());

          return arrayMove(curColumnOrder, oldIndex, newIndex);
        });
      }
    },
    [table],
  );

  return (
    <DndContext
      collisionDetection={collisionDetection}
      onDragEnd={handleDragEnd}
      onDragStart={handleDragStart}
      sensors={sensors}>
      <SortableContext items={draggableColumns} strategy={horizontalListSortingStrategy}>
        <DndStylesProvider>{children}</DndStylesProvider>
      </SortableContext>
      {createPortal(
        <DragOverlay dropAnimation={null} zIndex={zIndices.modalBody}>
          {activeColumn ? <ThDragOverlay<Entity> column={activeColumn} /> : null}
        </DragOverlay>,
        document.body,
      )}
    </DndContext>
  );
};

export default TableDndProvider;
