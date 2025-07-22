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
import { CACHES } from 'components/lookup-tables/fixtures';

import { attributes } from './constants';
import CacheList from './index';

const mockFetchPaginatedCaches = jest.fn(async () =>
  Promise.resolve({
    attributes,
    list: [...CACHES],
    pagination: {
      page: 1,
      total: CACHES.length,
      per_page: 20,
      count: 10,
      query: null,
    },
  }),
);

const mockDeleteCache = jest.fn(async () => Promise.resolve());

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
  useFetchCaches: () => ({
    fetchPaginatedCaches: mockFetchPaginatedCaches,
    cachesKeyFn: (searchParams: SearchParams) => ['caches', 'search', searchParams],
  }),
  useDeleteCache: () => ({
    deleteCache: mockDeleteCache,
    deletingCache: false,
  }),
}));

describe('Cache List', () => {
  it('should render a list of caches', async () => {
    render(<CacheList />);

    await screen.findByText(/0 cache title/i);
    screen.getByText(/0 cache description/i);
    screen.getByText(/0 cache name/i);
  });

  it('should show an actions menu', async () => {
    render(<CacheList />);

    await screen.findByRole('button', { name: CACHES[0].id });
  });

  it('should be able to edit a cache', async () => {
    render(<CacheList />);

    userEvent.click(await screen.findByRole('button', { name: CACHES[0].id }));

    await screen.findByRole('menuitem', { name: /edit/i });
  });

  it('should be able to delete a cache', async () => {
    render(<CacheList />);

    userEvent.click(await screen.findByRole('button', { name: CACHES[0].id }));
    userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    userEvent.click(await screen.findByRole('button', { name: /delete/i }));

    expect(mockDeleteCache).toHaveBeenLastCalledWith(CACHES[0].id);
  });
});
