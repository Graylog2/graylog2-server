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
import { Icon } from 'components/common';
import SearchForm from 'components/common/SearchForm';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

export type SortMode = 'alphabetical' | 'count';

const SORT_OPTIONS: Array<{ value: SortMode; label: string }> = [
  { value: 'alphabetical', label: 'Alphabetical' },
  { value: 'count', label: 'Count' },
];

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
  `,
);

const StyledSearchForm = styled(SearchForm)`
  flex: 1;

  .query,
  .input-container,
  .form-group {
    width: 100%;
    margin: 0;
  }
`;

const SortTrigger = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
  `,
);

type Props = {
  appSection: string;
  activeColumnTitle: string | undefined;
  sliceCol: string | undefined;
  searchQuery: string;
  onSearchQueryChange: (value: string) => void;
  onSearchReset: () => void;
  sortMode: SortMode;
  onSortModeChange: (mode: SortMode) => void;
};

const SliceFilters = ({
  appSection,
  activeColumnTitle,
  sliceCol,
  searchQuery,
  onSearchQueryChange,
  onSearchReset,
  sortMode,
  onSortModeChange,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const sortLabel = sortMode === 'count' ? 'Count' : 'A-Z';
  const sortIconName = sortMode === 'count' ? 'arrow_downward' : 'arrow_upward';
  const handleSearchChange = (value: string) => {
    onSearchQueryChange(value);
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_SEARCH_CHANGED, {
      app_section: appSection,
      event_details: {
        attribute_id: sliceCol,
        has_query: value.length > 0,
      },
    });
  };
  const handleSearchReset = () => {
    onSearchReset();
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_SEARCH_CHANGED, {
      app_section: appSection,
      event_details: {
        attribute_id: sliceCol,
        reset: true,
      },
    });
  };
  const handleSortChange = (mode: SortMode) => {
    if (mode === sortMode) {
      return;
    }

    onSortModeChange(mode);
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_SORT_CHANGED, {
      app_section: appSection,
      event_details: {
        attribute_id: sliceCol,
        sort_mode: mode,
      },
    });
  };

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
        <MenuItem key={option.value} onClick={() => handleSortChange(option.value)} active={sortMode === option.value}>
          {option.label}
        </MenuItem>
      ))}
    </DropdownButton>
  );

  return (
    <Controls>
      <ControlsRow>
        <StyledSearchForm
          onQueryChange={handleSearchChange}
          onReset={handleSearchReset}
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
