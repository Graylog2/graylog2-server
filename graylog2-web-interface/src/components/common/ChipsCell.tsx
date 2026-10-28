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

import { Label } from 'components/bootstrap';
import ExpandableCellList from 'components/common/ExpandableCellList';

const DEFAULT_COLLAPSED_COUNT = 1;

const HoverableLabel = styled(Label)(
  ({ theme }) => css`
    transition: background-color 0.1s ease-in-out;

    /* Override Label's flex label so the bare-string chip can be abbreviated with an ellipsis
       (overflow/text-overflow/white-space already come from Badge) instead of overflowing the cell. */
    .mantine-Badge-label {
      display: block;
    }

    /* Shrink as a flex item so the chip can truncate instead of leaking past the cell. */
    min-width: 0;
    max-width: 100%;

    /* Only clickable chips render as a <button> (via Label's own onClick prop) — restrict the
       hover/focus affordance to those so plain, non-clickable chips don't look interactive. */
    &:where(button):hover {
      background-color: ${theme.colors.gray[60]};
      cursor: pointer;
    }

    &:where(button):focus-visible {
      outline: 2px solid ${theme.colors.input.borderFocus};
      outline-offset: 2px;
      border-radius: 2px;
    }
  `,
);

type BaseProps = {
  items: ReadonlyArray<string> | undefined | null;
  collapsedCount?: number;
  truncate?: boolean;
  // Renders the chip's label content in place of the raw item; the chip itself (and its click
  // handling) stays owned by ChipsCell. Plain chips drop their raw-item title, so content that can be
  // cut off needs its own title; clickable chips already carry a "Filter by ..." title, so skip it there.
  renderItemContent?: (item: string) => React.ReactNode;
};

// itemLabel is required when chips are clickable so the aria-label reads as e.g.
// "Filter by tag" rather than "Filter by item".
// itemTitle names the item in that label when the raw value isn't readable on its own (e.g. a MITRE ID).
type Props =
  | (BaseProps & { onItemClick?: undefined; itemLabel?: never; itemTitle?: never })
  | (BaseProps & { onItemClick: (item: string) => void; itemLabel: string; itemTitle?: (item: string) => string });

const ChipsCell = ({
  items,
  onItemClick = undefined,
  collapsedCount = DEFAULT_COLLAPSED_COUNT,
  truncate = true,
  renderItemContent = undefined,
  itemLabel,
  itemTitle = undefined,
}: Props) => {
  const sorted = [...(items ?? [])].sort();

  const renderChip = (item: string) => {
    const content = renderItemContent ? renderItemContent(item) : item;

    if (!onItemClick) {
      return (
        <HoverableLabel bsStyle="default" title={renderItemContent ? undefined : item}>
          {content}
        </HoverableLabel>
      );
    }

    const filterLabel = `Filter by ${itemLabel} "${itemTitle ? itemTitle(item) : item}"`;

    return (
      <HoverableLabel
        bsStyle="default"
        onClick={(e: React.MouseEvent) => {
          e.stopPropagation();
          onItemClick(item);
        }}
        aria-label={filterLabel}
        title={filterLabel}>
        {content}
      </HoverableLabel>
    );
  };

  return (
    <ExpandableCellList
      items={sorted}
      renderItem={renderChip}
      getItemKey={(item) => item}
      collapsedCount={truncate ? collapsedCount : sorted.length}
      layout="inline"
    />
  );
};

export default ChipsCell;
