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
import { useState, useContext } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import { Spinner } from 'components/common';
import TableFetchContext from 'components/common/PaginatedEntityTable/TableFetchContext';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import SliceHeaderControls from './SliceHeaderControls';
import SliceFilters, { type SortMode } from './SliceFilters';
import SliceList from './SliceList';
import useSlices from './useSlices';
import type { FetchSlices } from './useFetchSlices';

type Slice = { value: string | number; count: number; title?: string };
export type Slices = Array<Slice>;

const Container = styled.div(
  ({ theme }) => css`
    min-width: 300px;
    border-right: 1px solid ${theme.colors.gray[90]};
    padding-right: ${theme.spacings.sm};
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

type Props = {
  appSection: string;
  columnSchemas: Array<ColumnSchema>;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string | undefined) => void;
  sliceRenderers?: { [col: string]: (value: string | number) => React.ReactNode } | undefined;
  fetchSlices: FetchSlices;
};

type SliceBodyProps = {
  appSection: string;
  sliceCol: string | undefined;
  activeSlice: string | undefined;
  activeColumnTitle: string | undefined;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string | undefined) => void;
  sliceRenderers?: { [col: string]: (value: string | number) => React.ReactNode } | undefined;
  fetchSlices: FetchSlices;
  sortMode: SortMode;
  onSortModeChange: (mode: SortMode) => void;
};

const SlicesOverview = ({
  appSection,
  sliceCol,
  activeSlice,
  activeColumnTitle,
  onChangeSlicing,
  sliceRenderers = undefined,
  fetchSlices,
  sortMode,
  onSortModeChange,
}: SliceBodyProps) => {
  const [showEmptySlices, setShowEmptySlices] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const sendTelemetry = useSendTelemetry();
  const { isLoading, hasEmptySlices, emptySliceCount, visibleNonEmptySlices, visibleEmptySlices } = useSlices({
    fetchSlices,
    searchQuery,
    sortMode,
  });
  const onToggleEmptySlices = () => {
    setShowEmptySlices((current) => {
      const next = !current;

      sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_EMPTY_VALUES_TOGGLED, {
        app_section: appSection,
        event_details: {
          attribute_id: sliceCol,
          show_empty_slices: next,
        },
      });

      return next;
    });
  };

  return (
    <>
      {isLoading && <Spinner />}
      {!isLoading && (
        <>
          <SliceFilters
            appSection={appSection}
            activeColumnTitle={activeColumnTitle}
            sliceCol={sliceCol}
            searchQuery={searchQuery}
            onSearchQueryChange={setSearchQuery}
            onSearchReset={() => setSearchQuery('')}
            sortMode={sortMode}
            onSortModeChange={onSortModeChange}
          />
          <SliceList
            slices={visibleNonEmptySlices}
            activeSlice={activeSlice}
            sliceCol={sliceCol}
            onChangeSlicing={onChangeSlicing}
            sliceRenderers={sliceRenderers}
            listTestId="slices-list"
          />
          <EmptySlicesHeader>
            {hasEmptySlices ? (
              <Button
                bsStyle="link"
                bsSize="sm"
                onClick={onToggleEmptySlices}
                title={showEmptySlices ? 'Hide empty slices' : 'Show empty slices'}>
                {showEmptySlices ? 'Hide empty slices' : 'Show empty slices'} ({emptySliceCount})
              </Button>
            ) : (
              <EmptySlicesLabel>Empty slices (0)</EmptySlicesLabel>
            )}
          </EmptySlicesHeader>
          {showEmptySlices && visibleEmptySlices.length > 0 && (
            <SliceList
              slices={visibleEmptySlices}
              activeSlice={activeSlice}
              sliceCol={sliceCol}
              onChangeSlicing={onChangeSlicing}
              sliceRenderers={sliceRenderers}
              keyPrefix="empty-"
              listTestId="empty-slices-list"
            />
          )}
        </>
      )}
    </>
  );
};

const Slicing = ({ appSection, columnSchemas, onChangeSlicing, sliceRenderers = undefined, fetchSlices }: Props) => {
  const {
    searchParams: { sliceCol, slice: activeSlice },
  } = useContext(TableFetchContext);
  const [sortMode, setSortMode] = useState<SortMode>('alphabetical');
  const activeColumn = columnSchemas.find(({ id }) => id === sliceCol);

  return (
    <Container>
      <SliceHeaderControls
        appSection={appSection}
        activeColumnTitle={activeColumn?.title}
        activeSlice={activeSlice}
        sliceCol={sliceCol}
        columnSchemas={columnSchemas}
        onChangeSlicing={onChangeSlicing}
      />
      <SlicesOverview
        key={sliceCol ?? 'no-slice'}
        appSection={appSection}
        sliceCol={sliceCol}
        activeSlice={activeSlice}
        activeColumnTitle={activeColumn?.title}
        onChangeSlicing={onChangeSlicing}
        sliceRenderers={sliceRenderers}
        fetchSlices={fetchSlices}
        sortMode={sortMode}
        onSortModeChange={setSortMode}
      />
    </Container>
  );
};

export default Slicing;
