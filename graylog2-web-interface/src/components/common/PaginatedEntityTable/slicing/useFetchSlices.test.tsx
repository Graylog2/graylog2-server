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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import TableFilterContext, {
  type TableFilterContextValue,
} from 'components/common/PaginatedEntityTable/TableFilterContext';
import TableFetchContextProvider from 'components/common/PaginatedEntityTable/TableFetchContextProvider';

import useFetchSlices from './useFetchSlices';

const tableFilterContext: TableFilterContextValue = {
  searchParams: {
    page: 1,
    pageSize: 10,
    query: '',
    sort: { attributeId: 'title', direction: 'asc' },
    sliceCol: 'status',
    slice: undefined,
    filters: undefined,
  },
  setQuery: jest.fn(),
  onChangeFilters: jest.fn(),
  onChangeSlicingFilter: jest.fn(),
  paginationState: { page: 1, pageSize: 10, resetPage: jest.fn(), setPagination: jest.fn() },
  resetFilters: jest.fn(),
};

describe('useFetchSlices', () => {
  it('does not share slices between layout variants with the same filters', async () => {
    let layoutVariant = 'active';
    const fetchSlices = jest.fn(() => Promise.resolve({ slices: [{ value: layoutVariant, title: layoutVariant, count: 1 }] }));
    const wrapper = ({ children }: React.PropsWithChildren) => (
      <TableFilterContext.Provider value={tableFilterContext}>
        <TableFetchContextProvider
          refetch={jest.fn()}
          attributes={[]}
          entityTableId="test_table"
          layoutVariant={layoutVariant}>
          {children}
        </TableFetchContextProvider>
      </TableFilterContext.Provider>
    );

    const { result, rerender } = renderHook(() => useFetchSlices(fetchSlices), { wrapper });

    await waitFor(() => expect(result.current.slices.map(({ value }) => value)).toEqual(['active']));

    layoutVariant = 'archived';
    rerender();

    await waitFor(() => expect(result.current.slices.map(({ value }) => value)).toEqual(['archived']));

    expect(fetchSlices).toHaveBeenCalledTimes(2);
  });
});
