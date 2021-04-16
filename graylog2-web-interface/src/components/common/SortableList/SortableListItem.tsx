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
import styled from 'styled-components';
import { CSS } from '@dnd-kit/utilities';
import { useSortable } from '@dnd-kit/sortable';
import type { DraggableSyntheticListeners } from '@dnd-kit/core';

import ListItem from './ListItem';

export type ListItemType = {
  id: string,
  title?: string | React.ReactElement,
}

export type DragHandleAttributes = Partial<{
  role: string;
  tabIndex: number;
  'aria-pressed': boolean | undefined;
  'aria-roledescription': string;
  'aria-describedby': string;
}>;

export type RenderCustomItem<ItemType extends ListItemType> = ({
  item,
  index,
  dragHandleAttributes,
  dragHandleListeners,
  ref,
  disableDragging,
} : {
  item: ItemType,
  index: number,
  dragHandleAttributes: DragHandleAttributes,
  dragHandleListeners: DraggableSyntheticListeners,
  className?: string,
  ref: React.Ref<any>,
  disableDragging?: boolean
}) => React.ReactNode;

const StyledListItem = styled(ListItem)(({
  $opacity,
  $transform,
  $transition,
}: {
  $opacity: React.CSSProperties['opacity'],
  $transform: React.CSSProperties['transform'],
  $transition: React.CSSProperties['transition'],
 }) => `
  opacity: ${$opacity};
  transform: ${$transform};
  transition: ${$transition};
`);

type Props<ItemType extends ListItemType> = {
  className?: string,
  disableDragging?: boolean,
  index: number,
  item: ItemType,
  renderCustomItem?: RenderCustomItem<ItemType>,
};

const SortableListItem = <ItemType extends ListItemType>({
  index,
  item,
  className,
  renderCustomItem,
  disableDragging,
}: Props<ItemType>) => {
  const {
    attributes,
    isDragging,
    listeners,
    setNodeRef,
    transform,
    transition,
  } = useSortable({ id: item.id });

  return (
    <StyledListItem className={className}
                    disableDragging={disableDragging}
                    dragHandleAttributes={attributes}
                    dragHandleListeners={listeners}
                    index={index}
                    item={item}
                    ref={setNodeRef}
                    renderCustomItem={renderCustomItem}
                    $transform={CSS.Transform.toString(transform)}
                    $transition={transition}
                    $opacity={isDragging ? 0.5 : 1} />
  );
};

SortableListItem.defaultProps = {
  className: undefined,
  renderCustomItem: undefined,
  disableDragging: false,
};

export default SortableListItem;
