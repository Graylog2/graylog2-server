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

const TagsContainer = styled.div(
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

    /* Override bootstrap Label defaults so a long tag wraps inside the cell. */
    display: inline-block;
    max-width: 100%;
    white-space: normal;
    overflow-wrap: anywhere;
    word-break: break-word;

    button:hover > & {
      background-color: ${theme.colors.gray[60]};
    }
  `,
);

const TagButton = styled.button(
  ({ theme }) => css`
    background: transparent;
    border: 0;
    padding: 0;
    cursor: pointer;

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

type Props = {
  tags: ReadonlyArray<string> | undefined | null;
  onTagClick?: (tag: string) => void;
  collapsedCount?: number;
  truncate?: boolean;
  emptyFallback?: React.ReactNode;
};

const TagsCell = ({
  tags,
  onTagClick = undefined,
  collapsedCount = DEFAULT_COLLAPSED_COUNT,
  truncate = true,
  emptyFallback = null,
}: Props) => {
  const [isExpanded, setIsExpanded] = useState(false);

  if (!tags?.length) {
    return <>{emptyFallback}</>;
  }

  const sortedTags = [...tags].sort();
  const isTruncated = truncate && sortedTags.length > collapsedCount;
  const visibleTags = !isExpanded && isTruncated ? sortedTags.slice(0, collapsedCount) : sortedTags;
  const hiddenCount = sortedTags.length - collapsedCount;

  const renderTag = (tag: string) => {
    if (!onTagClick) {
      return (
        <span key={tag}>
          <HoverableLabel bsStyle="default">{tag}</HoverableLabel>
        </span>
      );
    }

    return (
      <TagButton
        key={tag}
        type="button"
        onClick={(e: React.MouseEvent) => {
          e.stopPropagation();
          onTagClick(tag);
        }}
        aria-label={`Filter by tag "${tag}"`}
        title={`Filter by tag "${tag}"`}>
        <HoverableLabel bsStyle="default">{tag}</HoverableLabel>
      </TagButton>
    );
  };

  return (
    <Wrapper>
      <TagsContainer>{visibleTags.map(renderTag)}</TagsContainer>
      {isTruncated && (
        <ToggleButton
          type="button"
          aria-label={isExpanded ? 'Show fewer tags' : 'Show all tags'}
          title={isExpanded ? 'Show fewer tags' : 'Show all tags'}
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

export default TagsCell;
