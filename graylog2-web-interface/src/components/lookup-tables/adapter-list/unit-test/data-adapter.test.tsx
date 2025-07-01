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
import { render, screen } from 'wrappedTestingLibrary';

import type { SearchParams } from 'stores/PaginationTypes';

import { DATA_ADAPTERS, ERROR_STATE } from './fixtures';

import { attributes } from '../constants';
import DataAdapterList from '../index';

const mockFetchPaginatedCaches = jest.fn(async () =>
  Promise.resolve({
    attributes,
    list: [...DATA_ADAPTERS],
    pagination: {
      page: 1,
      total: DATA_ADAPTERS.length,
      per_page: 20,
      count: 10,
      query: null,
    },
  }),
);

const mockFetchErrors = jest.fn(async () => Promise.resolve({ ...ERROR_STATE }));

jest.mock('routing/QueryParams', () => ({
  useQueryParam: () => [undefined, () => { }],
}));

jest.mock('components/lookup-tables/hooks/useLookupTablesAPI', () => ({
  useFetchDataAdapters: () => ({
    fetchPaginatedDataAdapters: mockFetchPaginatedCaches,
    dataAdaptersKeyFn: (searchParams: SearchParams) => ['data-adapters', 'search', searchParams],
  }),
  useFetchErrors: () => ({
    fetchErrors: mockFetchErrors,
  }),
}));

describe('Data Adapter List', () => {
  it('should render a list of data adapters', async () => {
    render(<DataAdapterList />);

    await screen.findByText(/0 adapter title/i);
    screen.getByText(/0 adapter description/i);
    screen.getByText(/0 adapter name/i);
  });

  it('should show a warning icon on tables with errors', async () => {
    render(<DataAdapterList />);

    await screen.findByTestId('data-adapter-problem', { exact: true }, { timeout: 1500 });
  });
});
