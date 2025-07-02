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

import { LOOKUP_TABLES, CACHES, ADAPTERS, ERROR_STATE } from './fixtures';

import { attributes } from '../constants';
import LookupTableList from '../index';

const mockFetchPaginatedLookupTables = jest.fn(async () =>
  Promise.resolve({
    attributes,
    list: [...LOOKUP_TABLES],
    pagination: {
      page: 1,
      total: LOOKUP_TABLES.length,
      per_page: 20,
      count: 10,
      query: null,
    },
    meta: {
      caches: { ...CACHES },
      adapters: { ...ADAPTERS },
    },
  }),
);

const mockFetchErrors = jest.fn(async () => Promise.resolve({ ...ERROR_STATE }));
const mockDeleteLookupTable = jest.fn(async () => Promise.resolve());

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
  useQueryParam: () => [undefined, () => { }],
}));

jest.mock('components/lookup-tables/hooks/useLookupTablesAPI', () => ({
  useFetchLookupTables: () => ({
    fetchPaginatedLookupTables: mockFetchPaginatedLookupTables,
    lookupTablesKeyFn: jest.fn((searchParams: SearchParams) => ['lookup-tables', 'search', searchParams]),
  }),
  useDeleteLookupTable: () => ({
    deleteLookupTable: mockDeleteLookupTable,
    deletingLookupTable: false,
  }),
  useFetchErrors: () => ({
    fetchErrors: mockFetchErrors,
  }),
}));

describe('Lookup Table List', () => {
  it('should render a list of lookup tables', async () => {
    render(<LookupTableList />);

    await screen.findAllByRole('button', { name: /help/i });

    screen.getByText(/0 table title/i);
    screen.getByText(/0 table description/i);
    screen.getByText(/0 table name/i);

    screen.getByText(/0 cache title/i);
    screen.getByText(/1 cache title/i);

    screen.getByText(/0 adapter title/i);
    screen.getByText(/1 adapter title/i);
  });

  it('should show a warning icon on tables with errors', async () => {
    render(<LookupTableList />);

    await screen.findByTestId('lookup-table-problem', { exact: true }, { timeout: 1500 });
    screen.getByTestId('cache-problem');
    screen.getByTestId('data-adapter-problem');
  });

  it('should show an actions menu', async () => {
    render(<LookupTableList />);

    await screen.findByRole('button', { name: LOOKUP_TABLES[0].id });
  });

  it('should be able to edit a table', async () => {
    render(<LookupTableList />);

    userEvent.click(await screen.findByRole('button', { name: LOOKUP_TABLES[0].id }));

    await screen.findByRole('menuitem', { name: /edit/i });
  });

  it('should be able to delete a table', async () => {
    render(<LookupTableList />);

    userEvent.click(await screen.findByRole('button', { name: LOOKUP_TABLES[0].id }));
    userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    userEvent.click(await screen.findByRole('button', { name: /delete/i }));

    expect(mockDeleteLookupTable).toHaveBeenLastCalledWith(LOOKUP_TABLES[0].id);
  });
});
