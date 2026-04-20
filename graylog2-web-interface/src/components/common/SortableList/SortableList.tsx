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
import { useCallback, useState } from 'react';
import type { DragEndEvent } from '@dnd-kit/core';
import {
  PointerSensor,
  DndContext,
  closestCenter,
  useSensor,
  useSensors,
  MouseSensor,
  TouchSensor,
  KeyboardSensor,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  verticalListSortingStrategy,
  sortableKeyboardCoordinates,
} from '@dnd-kit/sortable';

import ListItemDragOverlay from 'components/common/SortableList/ListItemDragOverlay';

import type { ListItemType, CustomContentRender, CustomListItemRender } from './types';
import List from './List';

export type Props<ItemType extends ListItemType> = {
  alignItemContent?: 'flex-start' | 'center';
  customContentRender?: CustomContentRender<ItemType>;
  customListItemRender?: CustomListItemRender<ItemType>;
  disableDragging?: boolean;
  displayOverlayInPortal?: boolean;
  fullWidth?: boolean;
  items?: Array<ItemType>;
  onMoveItem: (newList: Array<ItemType>, sourceIndex: number, destinationIndex: number) => void;
};

/**
 * Component that renders a list of elements and let users manually
 * sort them by dragging and dropping them or by using the keyboard.
 *
 * `SortableList` just displays the provided items, consumers will need to store the state.
 * This way consumers can add or remove items easily.
 */
const SortableList = <ItemType extends ListItemType>({
  alignItemContent = undefined,
  customContentRender = undefined,
  customListItemRender = undefined,
  disableDragging = false,
  displayOverlayInPortal = false,
  fullWidth = false,
  items = [],
  onMoveItem,
}: Props<ItemType>) => {
  const [activeId, setActiveId] = useState<string | null>(null);
  const sensors = useSensors(
    useSensor(MouseSensor, {}),
    useSensor(TouchSensor, {}),
    useSensor(PointerSensor, {}),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragStart = useCallback((event) => {
    setActiveId(event.active.id);
  }, []);

  const handleDragEnd = useCallback(
    ({ active, over }: DragEndEvent) => {
      setActiveId(null);

      if (!over || active.id === over.id) {
        return;
      }

      const oldIndex = items.findIndex((item) => item.id === active.id);
      const newIndex = items.findIndex((item) => item.id === over.id);

      if (oldIndex !== -1 && newIndex !== -1 && oldIndex !== newIndex) {
        const newList = arrayMove(items, oldIndex, newIndex);
        onMoveItem(newList, oldIndex, newIndex);
      }
    },
    [items, onMoveItem],
  );

  return (
    <DndContext
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
      onDragStart={handleDragStart}
      sensors={sensors}>
      <SortableContext items={items.map((item) => item.id)} strategy={verticalListSortingStrategy}>
        <div style={{ width: fullWidth ? '100%' : undefined }}>
          <List
            alignItemContent={alignItemContent}
            customContentRender={customContentRender}
            customListItemRender={customListItemRender}
            disableDragging={disableDragging}
            items={items}
          />
        </div>
        <ListItemDragOverlay
          activeId={activeId}
          alignItemContent={alignItemContent}
          customContentRender={customContentRender}
          customListItemRender={customListItemRender}
          displayOverlayInPortal={displayOverlayInPortal}
          items={items}
        />
      </SortableContext>
    </DndContext>
  );
};

export default SortableList;
