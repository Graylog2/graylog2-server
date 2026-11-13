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
import styled, { css } from 'styled-components';

import CellListCollapsedRow from 'components/common/cell-list/CellListCollapsedRow';
import CellListExpanded from 'components/common/cell-list/CellListExpanded';
import CellListScrollArea from 'components/common/cell-list/CellListScrollArea';
import CellListToggle from 'components/common/cell-list/CellListToggle';
import useCellListToggle, { DEFAULT_COLLAPSED_COUNT } from 'components/common/cell-list/useCellListToggle';

type Layout = 'stacked' | 'inline';

const CollapsedItem = styled.div`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Items = styled.div<{ $layout: Layout }>(
  ({ theme, $layout }) => css`
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
  const { isExpanded, hiddenCount, toggle } = useCellListToggle(items?.length ?? 0, collapsedCount);

  if (!items?.length) {
    return null;
  }

  const renderAllItems = () => (
    <Items $layout={layout}>
      {items.map((item) => (
        <Item key={itemKey(item)} $layout={layout} title={itemTitle?.(item)}>
          {renderItem(item)}
        </Item>
      ))}
    </Items>
  );

  if (hiddenCount <= 0) {
    return renderAllItems();
  }

  if (!isExpanded) {
    return (
      <CellListCollapsedRow>
        {items.slice(0, collapsedCount).map((item) => (
          <CollapsedItem key={itemKey(item)} title={itemTitle?.(item)}>
            {renderItem(item)}
          </CollapsedItem>
        ))}
        <CellListToggle isExpanded={false} hiddenCount={hiddenCount} onToggle={toggle} />
      </CellListCollapsedRow>
    );
  }

  return (
    <CellListExpanded>
      <CellListScrollArea>{renderAllItems()}</CellListScrollArea>
      <CellListToggle isExpanded hiddenCount={hiddenCount} onToggle={toggle} />
    </CellListExpanded>
  );
};

export default ExpandableCellList;
