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

import { DropdownButton } from 'components/bootstrap';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import { Icon, SearchForm } from 'components/common';

const Controls = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    margin-bottom: ${theme.spacings.xs};
  `,
);

const ControlsRow = styled.div<{ $alignRight?: boolean }>(
  ({ theme, $alignRight }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    flex-wrap: nowrap;
    justify-content: ${$alignRight ? 'flex-end' : 'flex-start'};

    .search {
      flex: 1;
    }

    .query,
    .input-container,
    .form-group {
      width: 100%;
      margin: 0;
    }
  `,
);

const SortTrigger = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
  `,
);

export type SortMode = 'alphabetical' | 'count';
const SORT_OPTIONS: Array<{ value: SortMode; label: string }> = [
  { value: 'alphabetical', label: 'Alphabetical' },
  { value: 'count', label: 'Count' },
];

type Props = {
  activeColumnTitle: string | undefined;
  searchQuery: string;
  onSearchQueryChange: (value: string) => void;
  onSearchReset: () => void;
  sortMode: SortMode;
  onSortModeChange: (mode: SortMode) => void;
};

const SliceFilters = ({
  activeColumnTitle,
  searchQuery,
  onSearchQueryChange,
  onSearchReset,
  sortMode,
  onSortModeChange,
}: Props) => {
  const sortLabel = sortMode === 'count' ? 'Count' : 'A-Z';
  const sortIconName = sortMode === 'count' ? 'arrow_downward' : 'arrow_upward';

  const sortControl = (
    <DropdownButton
      bsSize="small"
      id="slicing-sort-dropdown"
      title={
        <SortTrigger>
          <Icon name={sortIconName} title={`Sort by ${sortLabel}`} />
          {sortLabel}
        </SortTrigger>
      }
      buttonTitle={`Sort by ${sortLabel}`}>
      {SORT_OPTIONS.map((option) => (
        <MenuItem key={option.value} onClick={() => onSortModeChange(option.value)} active={sortMode === option.value}>
          {option.label}
        </MenuItem>
      ))}
    </DropdownButton>
  );

  return (
    <Controls>
      <ControlsRow>
        <SearchForm
          onQueryChange={onSearchQueryChange}
          onReset={onSearchReset}
          placeholder={`Filter ${activeColumnTitle ?? 'slices'}`}
          query={searchQuery}
          queryWidth={0}
          buttonLeftMargin={0}
        />
        {sortControl}
      </ControlsRow>
    </Controls>
  );
};

export default SliceFilters;
