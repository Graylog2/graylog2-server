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
import styled, { css } from 'styled-components';

import { ListGroupItem } from 'components/bootstrap';
import { Icon } from 'components/common';

import type { DraggableProps, DragHandleProps, ListItemType, CustomListItemRender, CustomContentRender } from './types';

type Props<ItemType extends ListItemType> = {
  alignItemContent?: 'flex-start' | 'center'
  className?: string,
  customListItemRender?: CustomListItemRender<ItemType>,
  customContentRender?: CustomContentRender<ItemType>,
  disableDragging?: boolean,
  displayOverlayInPortal: boolean,
  draggableProps: DraggableProps,
  dragHandleProps: DragHandleProps,
  index: number,
  item: ItemType,
};

const StyledListGroupItem = styled(ListGroupItem)<{ $alignItemContent: 'flex-start' | 'center' }>(({ $alignItemContent }) => css`
  display: flex;
  align-items: ${$alignItemContent};
`);

const DragHandle = styled.div`
  margin-right: 5px;
`;

const ListItem = forwardRef(<ItemType extends ListItemType>({
  alignItemContent = 'flex-start',
  item,
  index,
  className,
  customListItemRender,
  customContentRender,
  disableDragging = false,
  draggableProps,
  dragHandleProps,
}: Props<ItemType>, ref) => {
  const itemContent = customContentRender ? customContentRender({ item, index }) : item.title;

  if (customListItemRender) {
    return (
      <>
        {customListItemRender({
          className,
          disableDragging,
          draggableProps: draggableProps,
          dragHandleProps: dragHandleProps,
          index,
          item,
          ref,
        })}
      </>
    );
  }

  return (
    <StyledListGroupItem $alignItemContent={alignItemContent}
                         ref={ref}
                         className={className}
                         containerProps={{ ...draggableProps }}>
      {!disableDragging && (
        <DragHandle {...dragHandleProps} data-testid={`sortable-item-${item.id}`}>
          <Icon name="drag_indicator" />
        </DragHandle>
      )}
      {itemContent}
    </StyledListGroupItem>
  );
});

export default ListItem;
