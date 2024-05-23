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

import type * as React from 'react';
import type { DraggableProvidedDraggableProps, DraggableProvidedDragHandleProps } from 'react-beautiful-dnd';

export type DraggableProps = DraggableProvidedDraggableProps
export type DragHandleProps = DraggableProvidedDragHandleProps

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
  draggableProps: DraggableProps;
  dragHandleProps: DragHandleProps;
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
