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
import type { DropResult } from 'react-beautiful-dnd';
import { DragDropContext, Droppable } from 'react-beautiful-dnd';
import { useCallback } from 'react';

import type { ListItemType, CustomContentRender, CustomListItemRender } from './types';
import List from './List';

const reorder = <ItemType extends ListItemType>(list: Array<ItemType>, startIndex, endIndex) => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);

  return result;
};

export type Props<ItemType extends ListItemType> = {
  alignItemContent?: 'flex-start' | 'center',
  customContentRender?: CustomContentRender<ItemType>,
  customListItemRender?: CustomListItemRender<ItemType>,
  disableDragging?: boolean,
  displayOverlayInPortal?: boolean,
  items?: Array<ItemType>
  onMoveItem: (newList: Array<ItemType>, sourceIndex: number, destinationIndex: number) => void,
}

/**
 * Component that renders a list of elements and let users manually
 * sort them by dragging and dropping them or by using the keyboard.
 *
 * `SortableList` just displays the provided items, consumers will need to store the state.
 * This way consumers can add or remove items easily.
 */
const SortableList = <ItemType extends ListItemType>({
  alignItemContent,
  customContentRender,
  customListItemRender,
  disableDragging = false,
  displayOverlayInPortal = false,
  items = [],
  onMoveItem,
}: Props<ItemType>) => {
  const onDragEnd = useCallback((result: DropResult) => {
    if (!result.destination) {
      return;
    }

    if (result.source.index !== result.destination.index) {
      const newList: Array<ItemType> = reorder(
        items,
        result.source.index,
        result.destination.index,
      );

      onMoveItem(newList, result.source.index, result.destination.index);
    }
  }, [items, onMoveItem]);

  return (
    <DragDropContext onDragEnd={onDragEnd}>
      <Droppable droppableId="droppable">
        {({ droppableProps, innerRef, placeholder }) => (
          <div {...droppableProps}
               ref={innerRef}>
            <List alignItemContent={alignItemContent}
                  items={items}
                  disableDragging={disableDragging}
                  displayOverlayInPortal={displayOverlayInPortal}
                  customContentRender={customContentRender}
                  customListItemRender={customListItemRender} />
            {placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>
  );
};

export default SortableList;
