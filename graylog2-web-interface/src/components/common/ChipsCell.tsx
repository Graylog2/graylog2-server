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

import { Badge, Label } from 'components/bootstrap';
import { Icon } from 'components/common';

const DEFAULT_COLLAPSED_COUNT = 3;

const Wrapper = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    flex-wrap: wrap;

    /* Fill the cell so max-width: 100% on chips resolves against the cell width. */
    width: 100%;
    min-width: 0;
  `,
);

const ChipsContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    gap: ${theme.spacings.xs};
    min-width: 0;
  `,
);

const HoverableLabel = styled(Label)(
  ({ theme }) => css`
    transition: background-color 0.1s ease-in-out;

    /* Override Label's flex label so the bare-string chip can be abbreviated with an ellipsis
       (overflow/text-overflow/white-space already come from Badge) instead of overflowing the cell. */
    .mantine-Badge-label {
      display: block;
    }

    button:hover > & {
      background-color: ${theme.colors.gray[60]};
    }
  `,
);

const ChipButton = styled.button(
  ({ theme }) => css`
    background: transparent;
    border: 0;
    padding: 0;
    cursor: pointer;

    /* Shrink as a flex item so the chip inside can truncate instead of leaking past the cell. */
    min-width: 0;
    max-width: 100%;

    /* Inner Label/Badge components set their own cursor; force pointer everywhere
       inside the button so the hover affordance is consistent. */
    & * {
      cursor: pointer;
    }

    &:focus-visible {
      outline: 2px solid ${theme.colors.input.borderFocus};
      outline-offset: 2px;
      border-radius: 2px;
    }
  `,
);

const ToggleButton = styled.button(
  ({ theme }) => css`
    background: transparent;
    border: 0;
    padding: 0;
    cursor: pointer;

    & * {
      cursor: pointer;
    }

    &:focus-visible {
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
  emptyFallback?: React.ReactNode;
  renderItem?: (item: string) => React.ReactNode;
};

// itemLabel is required when chips are clickable so the aria-label reads as e.g.
// "Filter by tag" rather than "Filter by item".
type Props =
  | (BaseProps & { onItemClick?: undefined; itemLabel?: never })
  | (BaseProps & { onItemClick: (item: string) => void; itemLabel: string });

const ChipsCell = ({
  items,
  onItemClick = undefined,
  collapsedCount = DEFAULT_COLLAPSED_COUNT,
  truncate = true,
  emptyFallback = null,
  renderItem: customRenderItem = undefined,
  itemLabel,
}: Props) => {
  const [isExpanded, setIsExpanded] = useState(false);

  if (!items?.length) {
    return <>{emptyFallback}</>;
  }

  const sorted = [...items].sort();
  const isTruncated = truncate && sorted.length > collapsedCount;
  const visible = !isExpanded && isTruncated ? sorted.slice(0, collapsedCount) : sorted;
  const hiddenCount = sorted.length - collapsedCount;

  const renderChip = (item: string) => {
    if (customRenderItem) {
      return <React.Fragment key={item}>{customRenderItem(item)}</React.Fragment>;
    }
    if (!onItemClick) {
      return (
        <HoverableLabel key={item} bsStyle="default" title={item}>
          {item}
        </HoverableLabel>
      );
    }

    return (
      <ChipButton
        key={item}
        type="button"
        onClick={(e: React.MouseEvent) => {
          e.stopPropagation();
          onItemClick(item);
        }}
        aria-label={`Filter by ${itemLabel} "${item}"`}
        title={`Filter by ${itemLabel} "${item}"`}>
        <HoverableLabel bsStyle="default">{item}</HoverableLabel>
      </ChipButton>
    );
  };

  return (
    <Wrapper>
      <ChipsContainer>{visible.map(renderChip)}</ChipsContainer>
      {isTruncated && (
        <ToggleButton
          type="button"
          aria-label={isExpanded ? 'Show fewer' : 'Show all'}
          title={isExpanded ? 'Show fewer' : 'Show all'}
          onClick={(e: React.MouseEvent) => {
            e.stopPropagation();
            setIsExpanded((v) => !v);
          }}>
          <Badge>{isExpanded ? <Icon name="keyboard_arrow_up" /> : `+ ${hiddenCount}`}</Badge>
        </ToggleButton>
      )}
    </Wrapper>
  );
};

export default ChipsCell;
