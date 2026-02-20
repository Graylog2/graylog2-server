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
import { Spinner } from 'components/common';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import SliceFilters, { type SortMode } from './SliceFilters';
import SliceList from './SliceList';
import useSlices from './useSlices';
import type { SliceRenderers } from './Slicing';
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
  const sendTelemetry = useSendTelemetry();
  const { isLoading, hasEmptySlices, emptySliceCount, visibleNonEmptySlices, visibleEmptySlices } = useSlices({
    fetchSlices,
    searchQuery,
    sortMode,
    sliceRenderers,
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
  );
};

export default SlicesOverview;
