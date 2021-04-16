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

import { ListGroupItem } from 'components/graylog';
import { Icon } from 'components/common';

export type ListItemType = {
  id: string,
  title?: string,
}

type DragHandleAttributes = Partial<{
  role: string;
  tabIndex: number;
  'aria-pressed': boolean | undefined;
  'aria-roledescription': string;
  'aria-describedby': string;
}>;

export type RenderListItem<ItemType extends ListItemType> = (
  item: ItemType,
  index: number,
  dragHandleAttributes: DragHandleAttributes,
  dragHandleListeners: DraggableSyntheticListeners,
) => React.ReactNode;

const DragHandleIcon = styled(Icon)`
  margin-right: 5px;
`;

export const ListItem = <ItemType extends ListItemType>({
  item,
  index,
  renderListItem,
  dragHandleAttributes,
  dragHandleListeners,
  className,
}: {
  item: ItemType,
  index: number,
  dragHandleAttributes?: DragHandleAttributes,
  dragHandleListeners?: DraggableSyntheticListeners,
  renderListItem?: RenderListItem<ItemType>,
  className?: string,
}) => {
  return (
    <>
      {renderListItem
        ? renderListItem(item, index, dragHandleAttributes, dragHandleListeners)
        : (
          <ListGroupItem className={className}>
            <DragHandleIcon name="bars" {...dragHandleAttributes} {...dragHandleListeners} />
            {'title' in item ? item.title : item.id}
          </ListGroupItem>
        )}
    </>
  );
};

ListItem.defaultProps = {
  dragHandleAttributes: {},
  dragHandleListeners: {},
  renderListItem: undefined,
  className: undefined,
};

type Props<ItemType extends ListItemType> = {
  className?: string,
  index: number,
  item: ItemType,
  renderListItem?: RenderListItem<ItemType>,
};

const ListItemWithSortStyling = styled(ListItem)(({
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

const SortableListItem = <ItemType extends ListItemType>({
  index,
  item,
  className,
  renderListItem,
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
    <div ref={setNodeRef}>
      <ListItemWithSortStyling item={item}
                               index={index}
                               className={className}
                               dragHandleAttributes={attributes}
                               dragHandleListeners={listeners}
                               renderListItem={renderListItem}
                               $transform={CSS.Transform.toString(transform)}
                               $transition={transition}
                               $opacity={isDragging ? 0.5 : 1} />
    </div>

  );
};

SortableListItem.defaultProps = {
  className: undefined,
  renderListItem: undefined,
};

export default SortableListItem;
