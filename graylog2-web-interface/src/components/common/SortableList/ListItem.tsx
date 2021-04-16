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
import type { DraggableSyntheticListeners } from '@dnd-kit/core';

import { ListGroupItem } from 'components/graylog';
import { Icon } from 'components/common';

import type { ListItemType, DragHandleAttributes, RenderListItem } from './SortableListItem';

const DragHandleIcon = styled(Icon)`
    margin-right: 5px;
`;

const ListItem = <ItemType extends ListItemType>({
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

export default ListItem;
