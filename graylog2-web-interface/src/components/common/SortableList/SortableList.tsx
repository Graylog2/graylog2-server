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
import { useState } from 'react';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import type { DragEndEvent } from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';

import SortableListItem from './SortableListItem';
import ListItemDragOverlay from './ListItemDragOverlay';
import type { RenderListItem, ListItemType } from './SortableListItem';

export type Props<ItemType extends ListItemType> = {
  items: Array<ItemType>,
  onSortChange: (newList: Array<ItemType>, oldItemIndex: number, newItemIndex: number) => void,
  renderListItem?: RenderListItem<ListItemType>,
}

const SortableList = <ListItem extends ListItemType>({ items, onSortChange, renderListItem }: Props<ListItem>) => {
  const [activeId, setActiveId] = useState<string>(null);
  const [list, setList] = useState(items);
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const onDragStart = (event) => {
    setActiveId(event.active.id);
  };

  const onDragEnd = (data: DragEndEvent) => {
    setActiveId(null);

    if (data.over) {
      const overIndex = list.findIndex((item) => data.over.id === item.id);
      const activeIndex = list.findIndex((item) => activeId === item.id);

      if (activeIndex !== overIndex) {
        const updatedList = arrayMove(list, activeIndex, overIndex);
        setList(updatedList);
        onSortChange(updatedList, activeIndex, overIndex);
      }
    }
  };

  return (
    <DndContext sensors={sensors}
                collisionDetection={closestCenter}
                onDragStart={onDragStart}
                onDragEnd={onDragEnd}
                onDragCancel={onDragEnd}>
      <SortableContext items={list.map(({ id }) => id)}
                       strategy={verticalListSortingStrategy}>
        {list.map((item, index) => (
          <SortableListItem item={item}
                            index={index}
                            renderListItem={renderListItem}
                            key={item.id} />
        ))}
      </SortableContext>

      <ListItemDragOverlay renderListItem={renderListItem} items={list} activeId={activeId} />
    </DndContext>
  );
};

SortableList.defaultProps = {
  renderListItem: undefined,
};

export default SortableList;
