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
import { DragOverlay, defaultDropAnimation } from '@dnd-kit/core';

import type { RenderCustomItem, ListItemType } from './SortableListItem';
import ListItem from './ListItem';

type Props = {
  activeId: ListItemType['id'],
  items: Array<ListItemType>,
  renderCustomItem?: RenderCustomItem<ListItemType>,
}

const ListItemDragOverlay = ({ activeId, items, renderCustomItem }: Props) => {
  const activeItemIndex = items.findIndex((item) => item.id === activeId);
  const activeItem = items[activeItemIndex];

  return (
    <DragOverlay dropAnimation={{ ...defaultDropAnimation, dragSourceOpacity: 0.5 }} zIndex={1100}>
      {activeId && (
        <ListItem index={activeItemIndex}
                  item={activeItem}
                  renderCustomItem={renderCustomItem} />
      )}
    </DragOverlay>
  );
};

export default ListItemDragOverlay;
