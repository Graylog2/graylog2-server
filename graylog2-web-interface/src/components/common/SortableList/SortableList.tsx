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
import { useState, useEffect } from 'react';
import { DragDropContext, Droppable } from 'react-beautiful-dnd';

import SortableListItem from './SortableListItem';
import type { RenderCustomItem, ListItemType } from './SortableListItem';

export type Props<ItemType extends ListItemType> = {
  disableDragging?: boolean,
  displayOverlayInPortal?: boolean,
  items: Array<ItemType>,
  onSortChange: (newList: Array<ItemType>, sourceIndex: number, destinationIndex: number) => void,
  renderCustomItem?: RenderCustomItem<ListItemType>
}

const reorder = <ItemType extends ListItemType>(list: Array<ItemType>, startIndex, endIndex) => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);

  return result;
};

const SortableList = <ItemType extends ListItemType>({
  items,
  renderCustomItem,
  disableDragging,
  onSortChange,
}: Props<ItemType>) => {
  const [list, setList] = useState(items);

  const onDragEnd = (result) => {
    if (!result.destination) {
      return;
    }

    const newList: Array<ItemType> = reorder(
      list,
      result.source.index,
      result.destination.index,
    );

    setList(newList);
    onSortChange(newList, result.source.index, result.destination.index);
  };

  useEffect(() => {
    if (list?.length !== items?.length) {
      setList(items);
    }
  }, [list, items]);

  return (
    <DragDropContext onDragEnd={onDragEnd}>
      <Droppable droppableId="droppable">
        {({ droppableProps, innerRef, placeholder }) => (
          <div {...droppableProps}
               ref={innerRef}>
            {items.map((item, index) => (
              <SortableListItem item={item}
                                index={index}
                                key={item.id}
                                renderCustomItem={renderCustomItem}
                                disableDragging={disableDragging} />
            ))}
            {placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>
  );
};

export default SortableList;
