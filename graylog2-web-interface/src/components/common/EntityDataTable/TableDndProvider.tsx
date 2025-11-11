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
import { useState, forwardRef, useCallback, useMemo } from 'react';
import { arrayMove, horizontalListSortingStrategy, SortableContext } from '@dnd-kit/sortable';
import type { Modifier, DragStartEvent, DragEndEvent } from '@dnd-kit/core';
import {
  closestCenter,
  DndContext,
  useSensors,
  useSensor,
  MouseSensor,
  TouchSensor,
  KeyboardSensor,
  DragOverlay,
} from '@dnd-kit/core';
import { getEventCoordinates } from '@dnd-kit/utilities';
import styled, { css } from 'styled-components';
import type { Table } from '@tanstack/react-table';

import zIndices from 'theme/z-indices';
import type { EntityBase, ColumnMetaContext } from 'components/common/EntityDataTable/types';
import Icon from 'components/common/Icon';
import { UTILITY_COLUMNS } from 'components/common/EntityDataTable/Constants';

const CustomDragOverlay = styled.div(
  ({ theme }) => css`
    background-color: ${theme.colors.global.background};
    z-index: ${zIndices.dropdownMenu};
    padding: ${theme.spacings.sm};
    width: min-content;
    font-weight: bold;
    white-space: nowrap;
    box-shadow:
      rgba(60, 64, 67, 0.3) 0 1px 2px 0,
      rgba(60, 64, 67, 0.15) 0 2px 6px 2px;

    border-radius: 5px;
    border: 1px solid ${theme.colors.variant.lighter.default};
    display: flex;
    align-items: center;
    line-height: 0;

    gap: ${theme.spacings.xs};
    cursor: grabbing;
  `,
);

const DragOverlayIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
  `,
);

const Item = forwardRef(({ title }: { title: string }, ref: React.ForwardedRef<HTMLDivElement>) => (
  <CustomDragOverlay ref={ref}>
    <DragOverlayIcon name="drag_pan" /> {title}
  </CustomDragOverlay>
));

const activeColumnTitle = <Entity extends EntityBase>(table: Table<Entity>, activeId: string | number) => {
  const activeColumnMeta = table.getAllColumns().find((col) => col.id === activeId)?.columnDef
    ?.meta as ColumnMetaContext<Entity>;

  return activeColumnMeta.label;
};

const snapOverlayToCursor: Modifier = ({ activatorEvent, draggingNodeRect, transform }) => {
  if (draggingNodeRect && activatorEvent) {
    const activatorCoordinates = getEventCoordinates(activatorEvent);

    if (!activatorCoordinates) {
      return transform;
    }

    const offsetX = activatorCoordinates.x - draggingNodeRect.left;
    const offsetY = activatorCoordinates.y - draggingNodeRect.top;

    return {
      ...transform,
      x: transform.x + offsetX - 20,
      y: transform.y + offsetY - 20,
    };
  }

  return transform;
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
      modifiers={[snapOverlayToCursor]}
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
