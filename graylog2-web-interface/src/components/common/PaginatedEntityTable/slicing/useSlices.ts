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

import { defaultCompare } from 'logic/DefaultCompare';

import type { Slices } from './Slicing';
import type { SortMode } from './SliceFilters';
import useFetchSlices, { type FetchSlices } from './useFetchSlices';

type Slice = Slices[number];

const sortSlices = (items: Slices, sortMode: SortMode, getSliceLabel: (slice: Slice) => string) =>
  [...items].sort((left, right) => {
    if (sortMode === 'count') {
      const countDiff = right.count - left.count;

      if (countDiff !== 0) {
        return countDiff;
      }
    }

    return defaultCompare(getSliceLabel(left), getSliceLabel(right));
  });

const getSliceLabel = (slice: Slice) => slice.title ?? String(slice.value);

const matchesQuery = (slice: Slice, query: string) => {
  const normalizedQuery = query.trim().toLowerCase();

  if (!normalizedQuery) {
    return true;
  }

  return getSliceLabel(slice).toLowerCase().includes(normalizedQuery);
};

type Props = {
  fetchSlices: FetchSlices;
  searchQuery: string;
  sortMode: SortMode;
};

const useSlices = ({ fetchSlices, searchQuery, sortMode }: Props) => {
  const { slices, isLoading } = useFetchSlices(fetchSlices);
  const filteredSlices = slices.filter((slice) => matchesQuery(slice, searchQuery));
  const nonEmptySlices = filteredSlices.filter((slice) => slice.count > 0);
  const emptySlices = filteredSlices.filter((slice) => slice.count === 0);

  return {
    isLoading,
    hasEmptySlices: emptySlices.length > 0,
    emptySliceCount: emptySlices.length,
    visibleNonEmptySlices: sortSlices(nonEmptySlices, sortMode, getSliceLabel),
    visibleEmptySlices: sortSlices(emptySlices, sortMode, getSliceLabel),
  };
};

export default useSlices;
