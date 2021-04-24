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

import type { ListItemType, CustomContentRender, CustomListItemRender } from './ListItem';
// import SortableListItem from './SortableListItem';
import List from './List';

const reorder = <ItemType extends ListItemType>(list: Array<ItemType>, startIndex, endIndex) => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);

  return result;
};

export type Props<ItemType extends ListItemType> = {
  customContentRender?: CustomContentRender<ItemType>,
  customListItemRender?: CustomListItemRender<ItemType>,
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
  customContentRender,
  customListItemRender,
  disableDragging,
  displayOverlayInPortal,
  items,
  onMoveItem,
}: Props<ItemType>) => {
  const onDragEnd = (result) => {
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
  };

  return (
    <DragDropContext onDragEnd={onDragEnd}>
      <Droppable droppableId="droppable">
        {({ droppableProps, innerRef, placeholder }) => (
          <div {...droppableProps}
               ref={innerRef}>
            <List items={items}
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
   * Custom list item content renderer. Allows rendering custom content next to the drag handle.
   * The default content is the item title. This method is not being called when `customListItemRender` is defined.
   */
  customContentRender: PropTypes.func,
  /**
   * Custom renderer for the complete list item. Can be used if `ListGroupItem` component is not suitable
   * or the drag handle needs to be displayed differently. When defined, `customContentRender` will not be called.
   */
  customListItemRender: PropTypes.func,
};

SortableList.defaultProps = {
  disableDragging: false,
  customContentRender: undefined,
  customListItemRender: undefined,
};

export default SortableList;
