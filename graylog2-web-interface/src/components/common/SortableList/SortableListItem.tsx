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
import { useSortable } from '@dnd-kit/sortable';
import type { Transform } from '@dnd-kit/utilities';
import { CSS } from '@dnd-kit/utilities';
import styled, { css } from 'styled-components';

import type { CustomListItemRender, ListItemType, CustomContentRender } from './types';
import ListItem from './ListItem';

const StyledListItem = styled(ListItem)<{ $isDragging: boolean; $transform: Transform; $transition: string }>(
  ({ $isDragging, $transform, $transition }) => css`
    transform: ${CSS.Transform.toString($transform)};
    transition: ${$transition};
    opacity: ${$isDragging ? 0.6 : 1};
  `,
);

type Props<ItemType extends ListItemType> = {
  alignItemContent?: 'flex-start' | 'center';
  className?: string;
  customListItemRender?: CustomListItemRender<ItemType>;
  customContentRender?: CustomContentRender<ItemType>;
  disableDragging?: boolean;
  index: number;
  item: ItemType;
};

const SortableListItem = <ItemType extends ListItemType>({
  alignItemContent = undefined,
  className = undefined,
  customContentRender = undefined,
  customListItemRender = undefined,
  disableDragging = false,
  index,
  item,
}: Props<ItemType>) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: item.id });

  return (
    <StyledListItem
      $isDragging={isDragging}
      $transform={transform}
      $transition={transition}
      alignItemContent={alignItemContent}
      className={className}
      customContentRender={customContentRender}
      customListItemRender={customListItemRender}
      disableDragging={disableDragging}
      dragHandleProps={{ ...attributes, ...listeners }}
      index={index}
      item={item}
      isDragging={isDragging}
      ref={setNodeRef}
    />
  );
};

export default SortableListItem;
