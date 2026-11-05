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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import { Badge } from 'components/bootstrap';
import Icon from 'components/common/Icon';
import { CELL_PADDING_HORIZONTAL } from 'components/common/EntityDataTable/Constants';

const DEFAULT_COLLAPSED_COUNT = 1;
// About ten one-line items. Longer lists scroll, so the row doesn't grow with the list.
const EXPANDED_MAX_HEIGHT = 240; // px

type Layout = 'stacked' | 'inline';

// Collapsed, the list stays on one line, so the row keeps its height.
const CollapsedRow = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    min-width: 0;
  `,
);

const CollapsedItem = styled.div`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Expanded = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
  `,
);

const Items = styled.div<{ $layout: Layout; $scrollable: boolean }>(
  ({ theme, $layout, $scrollable }) => css`
    display: flex;
    min-width: 0;
    ${$layout === 'stacked'
      ? css`
          flex-direction: column;
          align-items: flex-start;
          gap: ${theme.spacings.xxs};
        `
      : css`
          flex-wrap: wrap;
          gap: ${theme.spacings.xs};
        `}
    ${$scrollable &&
    css`
      max-height: ${EXPANDED_MAX_HEIGHT}px;
      overflow: hidden auto;

      /* Extends into the cell padding, so the scrollbar sits at the cell border. */
      margin-right: -${CELL_PADDING_HORIZONTAL}px;
      padding-right: ${CELL_PADDING_HORIZONTAL}px;
    `}
  `,
);

// Chips truncate themselves; stacked items get one line each, so every item has the same height.
const Item = styled.div<{ $layout: Layout }>(
  ({ $layout }) => css`
    min-width: 0;
    max-width: 100%;
    ${$layout === 'stacked' &&
    css`
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      /* With overflow: hidden the item may shrink below its content height, so a capped list would squeeze it. */
      flex-shrink: 0;
    `}
  `,
);

const ToggleBadge = styled(Badge)`
  flex-shrink: 0;
  align-self: flex-start;
`;

type Props<ItemType> = {
  items: ReadonlyArray<ItemType> | undefined | null;
  renderItem: (item: ItemType) => React.ReactNode;
  itemKey: (item: ItemType) => string;
  // Shown on hover, since long items are cut off.
  itemTitle?: (item: ItemType) => string;
  collapsedCount?: number;
  // `stacked` lists one item per line (e.g. entity links), `inline` wraps items next to each other (e.g. chips).
  layout?: Layout;
};

/**
 * Standard way to display a list in an entity data table cell: the first item(s) and the number of
 * remaining items. Expanding the list reveals the rest in the same cell.
 */
const ExpandableCellList = <ItemType,>({
  items,
  renderItem,
  itemKey,
  itemTitle = undefined,
  collapsedCount = DEFAULT_COLLAPSED_COUNT,
  layout = 'stacked',
}: Props<ItemType>) => {
  const [isExpanded, setIsExpanded] = useState(false);

  if (!items?.length) {
    return null;
  }

  const hiddenCount = items.length - collapsedCount;
  const renderAllItems = (scrollable: boolean) => (
    <Items $layout={layout} $scrollable={scrollable}>
      {items.map((item) => (
        <Item key={itemKey(item)} $layout={layout} title={itemTitle?.(item)}>
          {renderItem(item)}
        </Item>
      ))}
    </Items>
  );

  if (hiddenCount <= 0) {
    return renderAllItems(false);
  }

  const toggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsExpanded((cur) => !cur);
  };

  if (!isExpanded) {
    const title = `Show ${hiddenCount} more`;

    return (
      <CollapsedRow>
        {items.slice(0, collapsedCount).map((item) => (
          <CollapsedItem key={itemKey(item)} title={itemTitle?.(item)}>
            {renderItem(item)}
          </CollapsedItem>
        ))}
        <ToggleBadge color="gray" size="sm" onClick={toggle} title={title} aria-label={title}>
          +{hiddenCount}
          <Icon name="keyboard_arrow_down" size="sm" />
        </ToggleBadge>
      </CollapsedRow>
    );
  }

  return (
    <Expanded>
      {renderAllItems(true)}
      <ToggleBadge color="gray" size="sm" onClick={toggle}>
        Show less
        <Icon name="keyboard_arrow_up" size="sm" />
      </ToggleBadge>
    </Expanded>
  );
};

export default ExpandableCellList;
