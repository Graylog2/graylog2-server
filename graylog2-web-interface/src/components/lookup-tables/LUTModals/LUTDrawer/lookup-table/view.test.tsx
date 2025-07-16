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
import { LOOKUP_TABLES, CACHES_MAP, ADAPTERS_MAP, ERROR_STATE } from 'components/lookup-tables/fixtures';
import { attributes } from 'components/lookup-tables/lookup-table-list/constants';

import LookupTableDetails from './index';

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
      caches: { ...CACHES_MAP },
      adapters: { ...ADAPTERS_MAP },
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
    lookupTablesKeyFn: (searchParams: SearchParams) => ['lookup-tables', 'search', searchParams],
  }),
  useDeleteLookupTable: () => ({
    deleteLookupTable: mockDeleteLookupTable,
    deletingLookupTable: false,
  }),
  useFetchErrors: () => ({
    fetchErrors: mockFetchErrors,
  }),
}));
