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
import { createPortal } from 'react-dom';
import { Draggable } from 'react-beautiful-dnd';

import ListItem from './ListItem';
import type { ListItemType, RenderCustomItem } from './ListItem';

type Props<ItemType extends ListItemType> = {
  className?: string,
  disableDragging?: boolean,
  displayOverlayInPortal: boolean,
  index: number,
  item: ItemType,
  renderCustomItem?: RenderCustomItem<ItemType>,
};

const SortableListItem = <ItemType extends ListItemType>({
  item,
  index,
  className,
  renderCustomItem,
  disableDragging,
  displayOverlayInPortal,
}: Props<ItemType>) => (
  <Draggable draggableId={item.id} index={index}>
    {({ draggableProps, dragHandleProps, innerRef }, { isDragging }) => {
      const listItem = (
        <ListItem item={item}
                  index={index}
                  className={className}
                  ref={innerRef}
                  renderCustomItem={renderCustomItem}
                  disableDragging={disableDragging}
                  displayOverlayInPortal={displayOverlayInPortal}
                  draggableProps={draggableProps}
                  dragHandleProps={dragHandleProps} />
      );

      return (displayOverlayInPortal && isDragging)
        ? createPortal(listItem, document.body)
        : listItem;
    }}
  </Draggable>
  );

SortableListItem.defaultProps = {
  className: undefined,
  disableDragging: false,
  renderCustomItem: undefined,
};

export default SortableListItem;
