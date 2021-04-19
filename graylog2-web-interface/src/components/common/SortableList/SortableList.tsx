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
import { DragDropContext, Droppable } from 'react-beautiful-dnd';
import PropTypes from 'prop-types';

import type { ListItemType, CustomContentRender } from './ListItem';
import SortableListItem from './SortableListItem';

const reorder = <ItemType extends ListItemType>(list: Array<ItemType>, startIndex, endIndex) => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);

  return result;
};

export type Props<ItemType extends ListItemType> = {
  customContentRender?: CustomContentRender<ListItemType>
  disableDragging?: boolean,
  displayOverlayInPortal?: boolean,
  items: Array<ItemType>,
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
  disableDragging,
  displayOverlayInPortal,
  items,
  onMoveItem,
  customContentRender,
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
    onMoveItem(newList, result.source.index, result.destination.index);
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
                                customContentRender={customContentRender}
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

SortableList.propTypes = {
  /** Specifies if dragging and dropping is disabled or not. */
  disableDragging: PropTypes.bool,
  /**
   * Array of objects that will be displayed in the list. Each item is
   * expected to have an `id` and a `title` key. `id` must be unique
   * and will be used for sorting the item. `title` is used to display the
   * element name in the list.
   */
  items: PropTypes.arrayOf(PropTypes.object).isRequired,
  /**
   * Function that will be called when an item of the list was moved.
   * The function will receive the newly sorted list as an argument
   * and the source and destination index of the moved item.
   */
  onMoveItem: PropTypes.func.isRequired,
  /**
   * Custom content renderer for the SortableListItem. Will receive props to display custom drag handle.
   */
  customContentRender: PropTypes.func,
};

SortableList.defaultProps = {
  disableDragging: false,
  customContentRender: undefined,
};

export default SortableList;
