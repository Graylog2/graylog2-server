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
import type { RenderCustomItem, ListItemType } from './SortableListItem';

export type Props<ItemType extends ListItemType> = {
  disableDragging?: boolean,
  items: Array<ItemType>,
  onSortChange: (newList: Array<ItemType>, oldItemIndex: number, newItemIndex: number) => void,
  renderCustomItem?: RenderCustomItem<ListItemType>
}

const SortableList = <ItemType extends ListItemType>({
  disableDragging,
  items,
  onSortChange,
  renderCustomItem,
}: Props<ItemType>) => {
  const [activeId, setActiveId] = useState<string>(null);
  const [list, setList] = useState<Array<ItemType>>(items);
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
      const oldItemIndex = list.findIndex((item) => activeId === item.id);
      const newItemIndex = list.findIndex((item) => data.over.id === item.id);

      if (oldItemIndex !== newItemIndex) {
        const updatedList = arrayMove(list, oldItemIndex, newItemIndex);
        setList(updatedList);
        onSortChange(updatedList, oldItemIndex, newItemIndex);
      }
    }
  };

  return (
    <DndContext collisionDetection={closestCenter}
                onDragCancel={onDragEnd}
                onDragEnd={onDragEnd}
                onDragStart={onDragStart}
                sensors={sensors}>
      <SortableContext items={list.map(({ id }) => id)}
                       strategy={verticalListSortingStrategy}>
        {list.map((item, index) => (
          <SortableListItem disableDragging={disableDragging}
                            index={index}
                            item={item}
                            key={item.id}
                            renderCustomItem={renderCustomItem} />
        ))}
      </SortableContext>
      <ListItemDragOverlay activeId={activeId}
                           items={list}
                           renderCustomItem={renderCustomItem} />
    </DndContext>
  );
};

SortableList.defaultProps = {
  renderCustomItem: undefined,
  disableDragging: undefined,
};

export default SortableList;
