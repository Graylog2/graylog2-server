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
import { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import { useQuery } from '@tanstack/react-query';

import { DropdownButton, Button, ListGroup, Badge, ListGroupItem } from 'components/bootstrap';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import { defaultCompare } from 'logic/DefaultCompare';
import { defaultOnError } from 'util/conditional/onError';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import { Icon, SearchForm, Spinner } from 'components/common';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const Container = styled.div(
  ({ theme }) => css`
    min-width: 300px;
    border-right: 1px solid ${theme.colors.gray[90]};
    padding-right: ${theme.spacings.sm};

    .list-group {
      margin-bottom: 5px;
    }
  `,
);

const SliceHeader = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    justify-content: space-between;
  `,
);

const SlicesOverview = styled.div`
  margin-top: 10px;
`;

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

const SliceInner = styled.div`
  display: flex;
  justify-content: space-between;
`;

const SortTrigger = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
  `,
);

const StyledListGroupItem = styled(ListGroupItem)<{ $active: boolean }>(
  ({ $active }) => css`
    font-weight: ${$active ? 'bold' : 'normal'};
  `,
);

const EmptySlicesHeader = styled.div(
  ({ theme }) => css`
    margin: ${theme.spacings.sm} 0 ${theme.spacings.xs};
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: ${theme.spacings.xs};
    color: ${theme.colors.gray[50]};
    font-size: ${theme.fonts.size.small};
  `,
);

const EmptySlicesLabel = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
`;

const useSlices = ({
  column,
  query = undefined,
  filters = undefined,
  tmpFetchSlices,
}: {
  column: string;
  query: string;
  filters: UrlQueryFilters;
  tmpFetchSlices: any;
}) => {
  const { data, isLoading } = useQuery<Array<{ value: string | number; count: number; title?: string }>>({
    queryKey: ['slices', column, query, filters],
    queryFn: () => defaultOnError(tmpFetchSlices(column, query, filters), 'Error fetching security events'),
  });

  return { data, isLoading };
};

type SliceData = { value: string | number; count: number; title?: string };
type SortMode = 'alphabetical' | 'count';
const SORT_OPTIONS: Array<{ value: SortMode; label: string }> = [
  { value: 'alphabetical', label: 'Alphabetical' },
  { value: 'count', label: 'Count' },
];

type Props = {
  filters: UrlQueryFilters;
  query: string;
  appSection: string;
  sliceCol: string;
  columnSchemas: Array<ColumnSchema>;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string | undefined) => void;
  sliceRenderers?: { [col: string]: (value: string | number) => React.ReactNode } | undefined;
  tmpFetchSlices: any;
  activeSlice: string | undefined;
};

const Slicing = ({
  activeSlice,
  appSection,
  columnSchemas,
  filters,
  onChangeSlicing,
  query,
  sliceCol,
  sliceRenderers = undefined,
  tmpFetchSlices,
}: Props) => {
  const [showEmptySlices, setShowEmptySlices] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortMode, setSortMode] = useState<SortMode>('alphabetical');
  const sendTelemetry = useSendTelemetry();
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
        <MenuItem key={option.value} onClick={() => setSortMode(option.value)} active={sortMode === option.value}>
          {option.label}
        </MenuItem>
      ))}
    </DropdownButton>
  );
  const sliceableColumns = columnSchemas
    .filter((schema) => schema.sliceable)
    .sort(({ title: title1 }, { title: title2 }) => defaultCompare(title1, title2));
  const activeColumn = sliceableColumns.find(({ id }) => id === sliceCol);
  const onSliceColumn = (columnId: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_COLUMN_SELECTED_SECTION, {
      app_section: appSection,
      app_action_value: 'slice-column-select',
      event_details: { attribute_id: columnId },
    });
    onChangeSlicing(columnId);
  };
  const onRemoveSlicing = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_REMOVED, {
      app_section: appSection,
      app_action_value: 'slice-remove',
      event_details: { attribute_id: sliceCol },
    });
    onChangeSlicing(undefined, undefined);
  };

  useEffect(() => {
    setSearchQuery('');
    setShowEmptySlices(false);
  }, [sliceCol]);

  const { data: slices, isLoading } = useSlices({ column: sliceCol, query, filters, tmpFetchSlices });
  const hasEmptySlices = slices?.some((slice) => slice.count === 0) ?? false;
  const searchableColumns = new Set(['owner', 'associated_assets']);
  const shouldShowSearch = searchableColumns.has(sliceCol);
  const normalizedQuery = searchQuery.trim().toLowerCase();
  const getSliceLabel = (slice: SliceData) => slice.title ?? String(slice.value);
  const matchesQuery = (slice: SliceData) => {
    if (!normalizedQuery) {
      return true;
    }

    return getSliceLabel(slice).toLowerCase().includes(normalizedQuery);
  };
  const sortSlices = (items: Array<SliceData>) => {
    const sorted = [...items].sort((left, right) => {
      if (sortMode === 'count') {
        const countDiff = right.count - left.count;

        if (countDiff !== 0) {
          return countDiff;
        }
      }

      return defaultCompare(getSliceLabel(left), getSliceLabel(right));
    });

    return sorted;
  };
  const nonEmptySlices = (slices ?? []).filter((slice) => slice.count > 0).filter(matchesQuery);
  const emptySlices = (slices ?? []).filter((slice) => slice.count === 0).filter(matchesQuery);
  const visibleNonEmptySlices = sortSlices(nonEmptySlices);
  const visibleEmptySlices = sortSlices(emptySlices);
  const emptySliceCount = emptySlices.length;
  const renderSliceLabel = (slice: SliceData) =>
    sliceRenderers?.[sliceCol]?.(slice.value) ?? slice.title ?? String(slice.value);

  return (
    <Container>
      <SliceHeader>
        <DropdownButton bsSize="small" id="slicing-dropdown" title={activeColumn?.title ?? 'Slice by'}>
          <MenuItem header>Slice by</MenuItem>
          {sliceableColumns.map((schema) => (
            <MenuItem key={schema.id} onClick={() => onSliceColumn(schema.id)}>
              {schema.title}
            </MenuItem>
          ))}
          <MenuItem divider />
          <MenuItem onClick={onRemoveSlicing}>No slicing</MenuItem>
        </DropdownButton>
        {activeSlice && (
          <Button bsStyle="link" bsSize="sm" onClick={() => onChangeSlicing(sliceCol, undefined)}>
            Clear slice
          </Button>
        )}
      </SliceHeader>

      <SlicesOverview>
        {isLoading && <Spinner />}
        {!isLoading && (
          <>
            <Controls>
              {shouldShowSearch ? (
                <ControlsRow>
                  <SearchForm
                    onQueryChange={setSearchQuery}
                    onReset={() => setSearchQuery('')}
                    placeholder={`Filter ${activeColumn?.title ?? 'slices'}`}
                    query={searchQuery}
                    queryWidth={0}
                    buttonLeftMargin={0}
                  />
                  {sortControl}
                </ControlsRow>
              ) : (
                <ControlsRow $alignRight>{sortControl}</ControlsRow>
              )}
            </Controls>
            <ListGroup>
              {visibleNonEmptySlices.map((slice) => (
                <StyledListGroupItem
                  key={String(slice.value)}
                  onClick={() => onChangeSlicing(sliceCol, String(slice.value))}
                  $active={activeSlice === slice.value}>
                  <SliceInner>
                    {renderSliceLabel(slice)}
                    <Badge>{slice.count}</Badge>
                  </SliceInner>
                </StyledListGroupItem>
              ))}
            </ListGroup>
            <EmptySlicesHeader>
              {hasEmptySlices ? (
                <Button
                  bsStyle="link"
                  bsSize="sm"
                  onClick={() => setShowEmptySlices((current) => !current)}
                  title={showEmptySlices ? 'Hide empty slices' : 'Show empty slices'}>
                  {showEmptySlices ? 'Hide empty slices' : 'Show empty slices'} ({emptySliceCount})
                </Button>
              ) : (
                <EmptySlicesLabel>Empty slices (0)</EmptySlicesLabel>
              )}
            </EmptySlicesHeader>
            {showEmptySlices && visibleEmptySlices.length > 0 && (
              <ListGroup>
                {visibleEmptySlices.map((slice) => (
                  <StyledListGroupItem
                    key={`empty-${String(slice.value)}`}
                    onClick={() => onChangeSlicing(sliceCol, String(slice.value))}
                    $active={activeSlice === slice.value}>
                    <SliceInner>
                      {renderSliceLabel(slice)}
                      <Badge>{slice.count}</Badge>
                    </SliceInner>
                  </StyledListGroupItem>
                ))}
              </ListGroup>
            )}
          </>
        )}
      </SlicesOverview>
    </Container>
  );
};

export default Slicing;
