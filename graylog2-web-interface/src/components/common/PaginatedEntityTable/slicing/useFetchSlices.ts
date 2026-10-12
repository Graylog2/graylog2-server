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

import { useContext } from 'react';
import { useQuery } from '@tanstack/react-query';

import { defaultOnError } from 'util/conditional/onError';
import useTableFilterContext from 'components/common/PaginatedEntityTable/useTableFilterContext';
import type { Slice, SliceRenderers } from 'components/common/PaginatedEntityTable/slicing/Slicing';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import { slicesQueryKey } from 'components/common/PaginatedEntityTable/slicing/queryKeys';
import useOnRefresh from 'components/common/PaginatedEntityTable/useOnRefresh';
import TableFetchContext from 'components/common/PaginatedEntityTable/TableFetchContext';

export type FetchSlices = (
  column: string,
  query: string,
  filters: UrlQueryFilters,
) => Promise<{ slices: Array<Slice> }>;

const useFetchSlices = (fetchSlices: FetchSlices, sliceRenderers?: SliceRenderers) => {
  const {
    searchParams: { sliceCol, query, filters },
  } = useTableFilterContext();
  // Tables can share slice columns, and layout variants can fetch different slices for the same filters.
  const tableFetchContext = useContext(TableFetchContext);

  const { data, isLoading, refetch } = useQuery({
    queryKey: slicesQueryKey(
      sliceCol,
      query,
      filters,
      tableFetchContext?.entityTableId,
      tableFetchContext?.layoutVariant,
    ),
    queryFn: () =>
      defaultOnError(
        fetchSlices(sliceCol, query, filters).then(
          ({ slices }) => sliceRenderers?.[sliceCol]?.extendSlices?.(slices) ?? slices,
        ),
        'Error fetching table slices',
      ),
  });
  useOnRefresh(refetch);

  return { slices: data ?? [], isLoading, refetchSlices: refetch };
};

export default useFetchSlices;
