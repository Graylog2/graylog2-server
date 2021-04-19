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
import styled from 'styled-components';
import { Draggable } from 'react-beautiful-dnd';
import type { DraggableProvidedDraggableProps, DraggableProvidedDragHandleProps } from 'react-beautiful-dnd';

import { ListGroupItem } from 'components/graylog';

import { Icon } from '..';

export type ListItemType = {
  id: string,
  title?: string | React.ReactElement,
}

export type RenderCustomItem<ItemType extends ListItemType> = ({
  disableDragging,
  draggableProps,
  dragHandleProps,
  index,
  item,
  ref,
} : {
  className?: string,
  disableDragging?: boolean
  draggableProps: DraggableProvidedDraggableProps;
  dragHandleProps: DraggableProvidedDragHandleProps;
  index: number,
  item: ItemType,
  ref: React.Ref<any>,
}) => React.ReactNode;

type Props<ItemType extends ListItemType> = {
  className?: string,
  disableDragging?: boolean,
  displayOverlayInPortal: boolean,
  index: number,
  item: ItemType,
  renderCustomItem?: RenderCustomItem<ItemType>,
};

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  align-items: flex-start;
`;

const DragHandle = styled.div`
  margin-right: 5px;
`;

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
      const listItem = renderCustomItem
        ? renderCustomItem({
          className,
          disableDragging,
          draggableProps: draggableProps,
          dragHandleProps: dragHandleProps,
          index,
          item,
          ref: innerRef,
        }) : (
          <StyledListGroupItem ref={innerRef}
                               className={className}
                               containerProps={{ ...draggableProps }}>
            {!disableDragging && (
            <DragHandle {...dragHandleProps} data-testid={`sortable-item-${item.id}`}>
              <Icon name="bars" />
            </DragHandle>
            )}
            {'title' in item ? item.title : item.id}
          </StyledListGroupItem>
        );

      return (
        <>{(displayOverlayInPortal && isDragging) ? createPortal(listItem, document.body) : listItem}</>
      );
    }}
  </Draggable>
  );

SortableListItem.defaultProps = {
  className: undefined,
  disableDragging: false,
  renderCustomItem: undefined,
};

export default SortableListItem;
