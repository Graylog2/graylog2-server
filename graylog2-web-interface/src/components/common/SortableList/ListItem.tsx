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
import { forwardRef } from 'react';
import styled from 'styled-components';
import type { DraggableProvidedDraggableProps, DraggableProvidedDragHandleProps } from 'react-beautiful-dnd';

import { ListGroupItem } from 'components/graylog';
import { Icon } from 'components/common';

export type ListItemType = {
  id: string,
  title?: string | React.ReactElement,
}

export type CustomListItemRender<ItemType extends ListItemType> = ({
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

export type CustomContentRender<ItemType extends ListItemType> = ({
  index,
  item,
}: {
  index: number,
  item: ItemType,
}) => React.ReactNode;

type Props<ItemType extends ListItemType> = {
  className?: string,
  customListItemRender?: CustomListItemRender<ItemType>,
  customContentRender?: CustomContentRender<ItemType>,
  disableDragging?: boolean,
  displayOverlayInPortal: boolean,
  draggableProps: DraggableProvidedDraggableProps,
  dragHandleProps: DraggableProvidedDragHandleProps,
  index: number,
  item: ItemType,
};

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  align-items: flex-start;
`;

const DragHandle = styled.div`
  margin-right: 5px;
`;

const ListItem = forwardRef(<ItemType extends ListItemType>({
  item,
  index,
  className,
  customListItemRender,
  customContentRender,
  disableDragging,
  draggableProps,
  dragHandleProps,
}: Props<ItemType>, ref) => {
  const itemContent = customContentRender ? customContentRender({ item, index }) : item.title;

  return (
    <>{customListItemRender
      ? customListItemRender({
        className,
        disableDragging,
        draggableProps: draggableProps,
        dragHandleProps: dragHandleProps,
        index,
        item,
        ref,
      }) : (
        <StyledListGroupItem ref={ref}
                             className={className}
                             containerProps={{ ...draggableProps }}>
          {!disableDragging && (
            <DragHandle {...dragHandleProps} data-testid={`sortable-item-${item.id}`}>
              <Icon name="bars" />
            </DragHandle>
          )}
          {itemContent}
        </StyledListGroupItem>
      )}
    </>
  );
});

ListItem.defaultProps = {
  className: undefined,
  disableDragging: false,
  customListItemRender: undefined,
  customContentRender: undefined,
};

export default ListItem;
