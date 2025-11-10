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
import userEvent from '@testing-library/user-event';

import type { SearchParams } from 'stores/PaginationTypes';
import type { GenericEntityType } from 'logic/lookup-tables/types';
import { DATA_ADAPTERS, ERROR_STATE } from 'components/lookup-tables/fixtures';

import { attributes } from './constants';
import DataAdapterList from './index';

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
const mockDeleteDataAdapter = jest.fn(async () => Promise.resolve());

jest.mock('hooks/useScopePermissions', () => ({
  __esModule: true,
  default: jest.fn((entity: GenericEntityType) => {
    const scopes = {
      ILLUMINATE: { is_mutable: false },
      DEFAULT: { is_mutable: true },
    };

    return {
      loadingScopePermissions: false,
      scopePermissions: scopes[entity?._scope || 'DEFAULT'],
      checkPermissions: (inEntity: Partial<GenericEntityType>) => {
        const entityScope = inEntity?._scope?.toUpperCase() || 'DEFAULT';

        return scopes[entityScope].is_mutable;
      },
    };
  }),
}));

jest.mock('routing/QueryParams', () => ({
  useQueryParam: () => [undefined, () => {}],
}));

jest.mock('components/lookup-tables/hooks/useLookupTablesAPI', () => ({
  useFetchDataAdapters: () => ({
    fetchPaginatedDataAdapters: mockFetchPaginatedCaches,
    dataAdaptersKeyFn: (searchParams: SearchParams) => ['data-adapters', 'search', searchParams],
  }),
  useDeleteDataAdapter: () => ({
    deleteDataAdapter: mockDeleteDataAdapter,
    deletingDataAdapter: false,
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

  it('should show an actions menu', async () => {
    render(<DataAdapterList />);

    await screen.findByRole('button', { name: DATA_ADAPTERS[0].id });
  });

  it('should be able to edit a data adapter', async () => {
    render(<DataAdapterList />);

    userEvent.click(await screen.findByRole('button', { name: DATA_ADAPTERS[0].id }));

    await screen.findByRole('menuitem', { name: /edit/i });
  });

  it('should be able to delete a data adapter', async () => {
    render(<DataAdapterList />);

    userEvent.click(await screen.findByRole('button', { name: DATA_ADAPTERS[0].id }));
    userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    userEvent.click(await screen.findByRole('button', { name: /delete/i }));

    expect(mockDeleteDataAdapter).toHaveBeenLastCalledWith(DATA_ADAPTERS[0].id);
  });
});
