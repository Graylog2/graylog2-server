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

import type { ListItemType, RenderCustomItem } from './ListItem';
import SortableListItem from './SortableListItem';

const reorder = <ItemType extends ListItemType>(list: Array<ItemType>, startIndex, endIndex) => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);

  return result;
};

export type Props<ItemType extends ListItemType> = {
  disableDragging?: boolean,
  displayOverlayInPortal?: boolean,
  items: Array<ItemType>,
  onSortChange: (newList: Array<ItemType>, sourceIndex: number, destinationIndex: number) => void,
  renderCustomItem?: RenderCustomItem<ListItemType>
}

const SortableList = <ItemType extends ListItemType>({
  disableDragging,
  displayOverlayInPortal,
  items,
  onSortChange,
  renderCustomItem,
}: Props<ItemType>) => {
  const onDragEnd = (result) => {
    if (!result.destination) {
      return;
    }

    const newList: Array<ItemType> = reorder(
      items,
      result.source.index,
      result.destination.index,
    );
    onSortChange(newList, result.source.index, result.destination.index);
  };

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
                                disableDragging={disableDragging}
                                displayOverlayInPortal={displayOverlayInPortal} />
            ))}
            {placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>
  );
};

export default SortableList;
