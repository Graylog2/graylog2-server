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

import { Button } from 'components/bootstrap';
import { PaginatedList, Spinner } from 'components/common';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import SliceFilters, { type SortMode } from './SliceFilters';
import SliceList from './SliceList';
import useSlices from './useSlices';
import type { SliceRenderers, Slices } from './Slicing';
import type { FetchSlices } from './useFetchSlices';

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

const SlicesLists = styled.div`
  max-height: 700px;
  overflow: auto;
`;

const SLICES_PAGE_SIZE = 10;

const paginatedSlices = (slices: Slices, page: number, pageSize: number) => {
  const from = (page - 1) * pageSize;

  return slices.slice(from, from + pageSize);
};

type Props = {
  appSection: string;
  sliceCol: string | undefined;
  activeSlice: string | undefined;
  activeColumnTitle: string | undefined;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string | undefined) => void;
  sliceRenderers?: SliceRenderers;
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
}: Props) => {
  const [showEmptySlices, setShowEmptySlices] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [nonEmptyPage, setNonEmptyPage] = useState(1);
  const [emptyPage, setEmptyPage] = useState(1);
  const sendTelemetry = useSendTelemetry();
  const { isLoading, hasEmptySlices, emptySliceCount, visibleNonEmptySlices, visibleEmptySlices } = useSlices({
    fetchSlices,
    searchQuery,
    sortMode,
    sliceRenderers,
  });

  const currentNonEmptySlices = paginatedSlices(visibleNonEmptySlices, nonEmptyPage, SLICES_PAGE_SIZE);
  const currentEmptySlices = paginatedSlices(visibleEmptySlices, emptyPage, SLICES_PAGE_SIZE);

  const onSearchQueryChange = (newQuery: string) => {
    setSearchQuery(newQuery);
    setNonEmptyPage(1);
    setEmptyPage(1);
  };
  const onSortModeUpdate = (mode: SortMode) => {
    onSortModeChange(mode);
    setNonEmptyPage(1);
    setEmptyPage(1);
  };

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

      if (next) {
        setEmptyPage(1);
      }

      return next;
    });
  };

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <>
      <SliceFilters
        appSection={appSection}
        activeColumnTitle={activeColumnTitle}
        sliceCol={sliceCol}
        searchQuery={searchQuery}
        onSearchQueryChange={onSearchQueryChange}
        onSearchReset={() => onSearchQueryChange('')}
        sortMode={sortMode}
        onSortModeChange={onSortModeUpdate}
      />
      <SlicesLists>
        <PaginatedList
          activePage={nonEmptyPage}
          pageSize={SLICES_PAGE_SIZE}
          totalItems={visibleNonEmptySlices.length}
          showPageSizeSelect={false}
          hideFirstAndLastPageLinks
          useQueryParameter={false}
          onChange={(newPage, pageSize) => {
            void pageSize;
            setNonEmptyPage(newPage);
          }}>
          <SliceList
            slices={currentNonEmptySlices}
            activeSlice={activeSlice}
            sliceCol={sliceCol}
            onChangeSlicing={onChangeSlicing}
            sliceRenderers={sliceRenderers}
            listTestId="slices-list"
          />
        </PaginatedList>
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
          <PaginatedList
            activePage={emptyPage}
            hideFirstAndLastPageLinks
            pageSize={SLICES_PAGE_SIZE}
            totalItems={visibleEmptySlices.length}
            showPageSizeSelect={false}
            useQueryParameter={false}
            onChange={(newPage, pageSize) => {
              void pageSize;
              setEmptyPage(newPage);
            }}>
            <SliceList
              slices={currentEmptySlices}
              activeSlice={activeSlice}
              sliceCol={sliceCol}
              onChangeSlicing={onChangeSlicing}
              sliceRenderers={sliceRenderers}
              keyPrefix="empty-"
              listTestId="empty-slices-list"
            />
          </PaginatedList>
        )}
      </SlicesLists>
    </>
  );
};

export default SlicesOverview;
