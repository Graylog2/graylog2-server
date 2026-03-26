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
import type { Slices, SliceRenderers } from 'components/common/PaginatedEntityTable/slicing/Slicing';

import { ALPHABETICAL_SORT, type SortDirection, type SortMode } from './SliceFilters';
import useFetchSlices, { type FetchSlices } from './useFetchSlices';

type Slice = Slices[number];

const compareNullSlices = (left: Slice, right: Slice) => {
  if (left.value === null && right.value !== null) {
    return -1;
  }

  if (left.value !== null && right.value === null) {
    return 1;
  }

  return 0;
};

const compareNullableValues = (
  left: string | number | boolean | null | undefined,
  right: string | number | boolean | null | undefined,
) => {
  if (left == null && right == null) {
    return 0;
  }

  if (left == null) {
    return 1;
  }

  if (right == null) {
    return -1;
  }

  if (typeof left === 'number' && typeof right === 'number') {
    return left - right;
  }

  return defaultCompare(String(left), String(right));
};

const sortSlices = (
  items: Slices,
  sortMode: SortMode,
  sortDirection: SortDirection,
  getSliceLabel: (slice: Slice) => string,
) =>
  [...items].sort((left, right) => {
    const nullSliceComparison = compareNullSlices(left, right);

    if (nullSliceComparison !== 0) {
      return nullSliceComparison;
    }

    let comparison = 0;

    if (sortMode === ALPHABETICAL_SORT) {
      comparison = defaultCompare(getSliceLabel(left), getSliceLabel(right));
    } else {
      comparison = compareNullableValues(left.meta?.[sortMode], right.meta?.[sortMode]);
    }

    if (comparison !== 0) {
      return sortDirection === 'asc' ? comparison : -comparison;
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
  activeSlice: string | undefined;
  searchQuery: string;
  sortMode: SortMode;
  sortDirection: SortDirection;
  sliceRenderers?: SliceRenderers;
};

const isSelectedSlice = (slice: Slice, activeSlice: string | undefined) =>
  activeSlice !== undefined && String(slice.value) === String(activeSlice);

const addMissingSelectedSlice = (slices: Slices, activeSlice: string | undefined): Slices => {
  if (activeSlice === undefined) {
    return slices;
  }

  const selectedSliceExists = slices.some((slice) => isSelectedSlice(slice, activeSlice));

  if (selectedSliceExists) {
    return slices;
  }

  return [...slices, { value: activeSlice, count: 0 }];
};

const useSlices = ({
  fetchSlices,
  activeSlice,
  searchQuery,
  sortMode,
  sortDirection,
  sliceRenderers = undefined,
}: Props) => {
  const { slices, isLoading, refetchSlices } = useFetchSlices(fetchSlices, sliceRenderers);
  const slicesWithSelected = addMissingSelectedSlice(slices, activeSlice);
  const filteredSlices = slicesWithSelected.filter((slice) => matchesQuery(slice, searchQuery));
  const nonEmptySlices = filteredSlices.filter((slice) => slice.count > 0);
  const emptySlices = filteredSlices.filter((slice) => slice.count === 0);

  return {
    isLoading,
    refetchSlices,
    hasEmptySlices: emptySlices.length > 0,
    emptySliceCount: emptySlices.length,
    visibleNonEmptySlices: sortSlices(nonEmptySlices, sortMode, sortDirection, getSliceLabel),
    visibleEmptySlices: sortSlices(emptySlices, sortMode, sortDirection, getSliceLabel),
  };
};

export default useSlices;
