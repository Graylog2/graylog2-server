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
import styled from 'styled-components';

import ListItem from './ListItem';
import type { ListItemType, CustomContentRender, CustomListItemRender } from './ListItem';

type Props<ItemType extends ListItemType> = {
  className?: string,
  customListItemRender?: CustomListItemRender<ItemType>,
  customContentRender?: CustomContentRender<ItemType>,
  disableDragging?: boolean,
  displayOverlayInPortal: boolean,
  index: number,
  item: ItemType,
};

const StyledListItem = styled(ListItem)(({ $isDragging }: { $isDragging: boolean }) => `
  box-shadow: ${$isDragging ? 'rgba(0, 0, 0, 0.24) 0px 3px 8px' : 'none'};
`);

const SortableListItem = <ItemType extends ListItemType>({
  className,
  customContentRender,
  customListItemRender,
  disableDragging,
  displayOverlayInPortal,
  index,
  item,
}: Props<ItemType>) => (
  <Draggable draggableId={item.id} index={index}>
    {({ draggableProps, dragHandleProps, innerRef }, { isDragging }) => {
      const listItem = (
        <StyledListItem item={item}
                        index={index}
                        className={className}
                        ref={innerRef}
                        customContentRender={customContentRender}
                        customListItemRender={customListItemRender}
                        disableDragging={disableDragging}
                        displayOverlayInPortal={displayOverlayInPortal}
                        draggableProps={draggableProps}
                        dragHandleProps={dragHandleProps}
                        $isDragging={isDragging} />
      );

      return (displayOverlayInPortal && isDragging)
        ? createPortal(listItem, document.body)
        : listItem;
    }}
  </Draggable>
  );

SortableListItem.defaultProps = {
  className: undefined,
  customContentRender: undefined,
  customListItemRender: undefined,
  disableDragging: false,
};

export default SortableListItem;
