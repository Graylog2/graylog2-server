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
import { DragOverlay } from '@dnd-kit/core';
import styled from 'styled-components';
import { createPortal } from 'react-dom';

import zIndices from 'theme/z-indices';
import ListItem from 'components/common/SortableList/ListItem';
import type { CustomContentRender, CustomListItemRender, ListItemType } from 'components/common/SortableList/types';

const SortableListItemOverlay = styled(ListItem)`
  box-shadow:
    rgb(0 0 0 / 5%) 0 1px 3px 0,
    rgb(0 0 0 / 5%) 0 28px 23px -7px,
    rgb(0 0 0 / 4%) 0 12px 12px -7px;
`;

type Props<ItemType extends ListItemType> = {
  activeId: string | null;
  alignItemContent?: 'flex-start' | 'center';
  customContentRender?: CustomContentRender<ItemType>;
  customListItemRender?: CustomListItemRender<ItemType>;
  displayOverlayInPortal: boolean;
  items: Array<ItemType>;
};

const ListItemDragOverlay = <ItemType extends ListItemType>({
  activeId,
  alignItemContent = undefined,
  customContentRender = undefined,
  customListItemRender = undefined,
  displayOverlayInPortal,
  items,
}: Props<ItemType>) => {
  const activeItemIndex = activeId ? items.findIndex((item) => item.id === activeId) : -1;
  const activeItem = activeId ? items[activeItemIndex] : null;

  const dragOverlay = (
    <DragOverlay zIndex={zIndices.modalBody}>
      {activeItem ? (
        <SortableListItemOverlay
          isDragging
          alignItemContent={alignItemContent}
          customContentRender={customContentRender}
          customListItemRender={customListItemRender}
          dragHandleProps={{}}
          index={activeItemIndex}
          item={activeItem}
        />
      ) : null}
    </DragOverlay>
  );

  return displayOverlayInPortal ? createPortal(dragOverlay, document.body) : dragOverlay;
};

export default ListItemDragOverlay;
