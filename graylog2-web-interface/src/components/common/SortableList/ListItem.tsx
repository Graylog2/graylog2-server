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
import DragHandle from 'components/common/SortableList/DragHandle';

import type { ListItemType, CustomListItemRender, CustomContentRender, DragHandleProps } from './types';

type Props<ItemType extends ListItemType> = {
  alignItemContent?: 'flex-start' | 'center';
  className?: string;
  customContentRender?: CustomContentRender<ItemType>;
  customListItemRender?: CustomListItemRender<ItemType>;
  disableDragging?: boolean;
  dragHandleProps: DragHandleProps;
  index: number;
  isDragging: boolean;
  item: ItemType;
};

const StyledListGroupItem = styled(ListGroupItem)<{
  $alignItemContent: 'flex-start' | 'center';
}>(
  ({ $alignItemContent }) => css`
    display: flex;
    align-items: ${$alignItemContent};
  `,
);

const ListItem = <ItemType extends ListItemType>(
  {
    alignItemContent = 'flex-start',
    className = undefined,
    customContentRender = undefined,
    customListItemRender = undefined,
    disableDragging = false,
    dragHandleProps,
    index,
    isDragging,
    item,
  }: Props<ItemType>,
  ref: React.ForwardedRef<HTMLLIElement>,
) => {
  const itemContent = customContentRender ? customContentRender({ item, index }) : item.title;

  const dragHandle = disableDragging ? null : (
    <DragHandle dragHandleProps={dragHandleProps} itemTitle={item.title} index={index} isDragging={isDragging} />
  );

  if (customListItemRender) {
    return (
      <>
        {customListItemRender({
          className,
          disableDragging,
          dragHandle,
          index,
          item,
          ref,
        })}
      </>
    );
  }

  return (
    <StyledListGroupItem $alignItemContent={alignItemContent} ref={ref} className={className}>
      {dragHandle}
      {itemContent}
    </StyledListGroupItem>
  );
};

export default forwardRef(ListItem);
