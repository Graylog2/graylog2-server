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
import { useMemo, useContext } from 'react';
import styled, { css } from 'styled-components';

import type { ColumnSchema } from 'components/common/EntityDataTable';
import TableFetchContext from 'components/common/PaginatedEntityTable/TableFetchContext';

import SliceHeaderControls from './SliceHeaderControls';
import { DEFAULT_SORT_OPTIONS } from './SliceFilters';
import SlicesOverview from './SlicesOverview';
import type { FetchSlices } from './useFetchSlices';

export type Slice = {
  value: string | number;
  count: number;
  title?: string;
  type?: unknown;
  meta?: Record<string, string | number | boolean | null | undefined>;
};
export type SliceRenderer = {
  extendSlices?: (slices: Array<Slice>) => Array<Slice>;
  render?: (slice: Slice) => React.ReactNode;
};
export type SliceRenderers = { [col: string]: SliceRenderer };
export type Slices = Array<Slice>;

const Container = styled.div(
  ({ theme }) => css`
    width: 400px;
    border-right: 1px solid ${theme.colors.gray[90]};
    padding-right: ${theme.spacings.sm};
  `,
);

type Props = {
  appSection: string;
  columnSchemas: Array<ColumnSchema>;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string | undefined) => void;
  sliceRenderers?: SliceRenderers;
  fetchSlices: FetchSlices;
};

const Slicing = ({ appSection, columnSchemas, onChangeSlicing, sliceRenderers = undefined, fetchSlices }: Props) => {
  const {
    searchParams: { sliceCol, slice: activeSlice },
  } = useContext(TableFetchContext);
  const activeColumn = columnSchemas.find(({ id }) => id === sliceCol);
  const sortOptions = useMemo(
    () => [
      ...DEFAULT_SORT_OPTIONS,
      ...(activeColumn?.slice_sort_options?.map((option) => ({ value: option.value, label: option.title })) ?? []),
    ],
    [activeColumn],
  );

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
        sortOptions={sortOptions}
      />
    </Container>
  );
};

export default Slicing;
