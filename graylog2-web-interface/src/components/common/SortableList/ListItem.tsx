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
import type { DraggableSyntheticListeners } from '@dnd-kit/core';

import { ListGroupItem } from 'components/graylog';
import { Icon } from 'components/common';

import type { ListItemType, DragHandleAttributes, RenderCustomItem } from './SortableListItem';

type Props<ItemType extends ListItemType> = {
  className?: string,
  disableDragging?: boolean,
  dragHandleAttributes?: DragHandleAttributes,
  dragHandleListeners?: DraggableSyntheticListeners,
  index: number,
  item: ItemType,
  renderCustomItem?: RenderCustomItem<ItemType>,
}

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  align-items: flex-start;
`;

const DragHandleIcon = styled(Icon)`
  margin-top: 3px;
  margin-right: 5px;
`;

const ListItem = forwardRef(<ItemType extends ListItemType>({
  className,
  disableDragging,
  dragHandleAttributes,
  dragHandleListeners,
  index,
  item,
  renderCustomItem,
}: Props<ItemType>, ref) => (
  <>
    {renderCustomItem
      ? renderCustomItem({ item, index, dragHandleAttributes, dragHandleListeners, className, ref, disableDragging })
      : (
        <div ref={ref} className={className}>
          <StyledListGroupItem>
            {!disableDragging && (
              <DragHandleIcon name="bars" {...dragHandleAttributes} {...dragHandleListeners} />
            )}
            {'title' in item ? item.title : item.id}
          </StyledListGroupItem>
        </div>
      )}
  </>
  ));

ListItem.defaultProps = {
  className: undefined,
  disableDragging: false,
  dragHandleAttributes: undefined,
  dragHandleListeners: undefined,
  renderCustomItem: undefined,
};

export default ListItem;
