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

import { ModalProvider } from 'components/lookup-tables/contexts/ModalContext';
import type { GenericEntityType, LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';
import {
  LOOKUP_TABLES,
  CACHES,
  DATA_ADAPTERS,
  ERRORS_CONTEXT_VALUE,
  UNSUPPORTED_PREVIEW,
  SUPPORTED_PREVIEW,
  TEST_KEY_RESULT,
  CACHE_PLUGIN as MOCK_CACHE_PLUGIN,
  DATA_ADAPTER_PLUGIN as MOCK_DATA_ADAPTER_PLUGIN,
} from 'components/lookup-tables/fixtures';

import LookupTableDetails from './index';

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

jest.mock('hooks/usePluginEntities', () => ({
  __esModule: true,
  default: jest.fn((pluginType: string) => {
    if (pluginType === 'lookupTableCaches') return [MOCK_CACHE_PLUGIN];
    if (pluginType === 'lookupTableAdapters') return [MOCK_DATA_ADAPTER_PLUGIN];

    return [];
  }),
}));

jest.mock('routing/QueryParams', () => ({
  useQueryParam: () => [undefined, () => {}],
}));

const mockPurgeLookupTableKey = jest.fn(async () => Promise.resolve());
const mockPurgeAllLookupTableKey = jest.fn(async () => Promise.resolve());
const mockUseFetchLookupPreview = jest.fn(() => ({
  lookupPreview: UNSUPPORTED_PREVIEW,
}));
const mockTestLookupTableKey = jest.fn(async () => Promise.resolve(null));
const mockUseErrorsContext = jest.fn(() => ERRORS_CONTEXT_VALUE);

jest.mock('components/lookup-tables/hooks/useLookupTablesAPI', () => ({
  usePurgeLookupTableKey: () => ({
    purgeLookupTableKey: mockPurgeLookupTableKey,
  }),
  usePurgeAllLookupTableKey: () => ({
    purgeAllLookupTableKey: mockPurgeAllLookupTableKey,
  }),
  useFetchLookupPreview: () => mockUseFetchLookupPreview(),
  useTestLookupTableKey: () => ({
    testLookupTableKey: mockTestLookupTableKey,
  }),
}));

jest.mock('components/lookup-tables/contexts/ErrorsContext', () => ({
  useErrorsContext: () => mockUseErrorsContext(),
}));

function renderView(table: LookupTable, cache: LookupTableCache, dataAdapter: LookupTableAdapter) {
  return render(
    <ModalProvider>
      <LookupTableDetails table={table} cache={cache} dataAdapter={dataAdapter} />
    </ModalProvider>,
  );
}

describe('Lookup Table Details', () => {
  it('should render lookup table details', async () => {
    const table: LookupTable = {
      ...LOOKUP_TABLES[0],
      default_single_value: '0',
      default_single_value_type: 'NUMBER',
      default_multi_value: '{}',
      default_multi_value_type: 'OBJECT',
    };
    const cache = CACHES[0];
    const dataAdapter = DATA_ADAPTERS[0];

    renderView(table, cache, dataAdapter);

    await screen.findByText(table.description);
    screen.getByText(cache.title);
    screen.getByText(dataAdapter.title);
    screen.getByText(/default single value/i);
    screen.getByText('(number)');
    screen.getByText(/default multi value/i);
    screen.getByText('(object)');
  });

  it('should purge all keys', async () => {
    renderView(LOOKUP_TABLES[0], CACHES[0], DATA_ADAPTERS[0]);

    userEvent.click(await screen.findByRole('button', { name: /Purge all/i }));

    expect(mockPurgeAllLookupTableKey).toHaveBeenCalledWith(LOOKUP_TABLES[0]);
  });

  it('shuld purge a key', async () => {
    const testKeyValue = 'test_key';
    renderView(LOOKUP_TABLES[0], CACHES[0], DATA_ADAPTERS[0]);

    userEvent.type(await screen.findByRole('textbox', { name: /key/i }), testKeyValue);
    userEvent.click(await screen.findByRole('button', { name: /Purge key/i }));

    expect(mockPurgeLookupTableKey).toHaveBeenCalledWith({ table: LOOKUP_TABLES[0], key: testKeyValue });
  });

  it("should show a message when preview isn't supported", async () => {
    renderView(LOOKUP_TABLES[0], CACHES[0], DATA_ADAPTERS[0]);

    await screen.findByText(/This lookup table doesn't support keys preview/i);
  });

  it('should show the validation error message', async () => {
    mockUseFetchLookupPreview.mockReturnValue({ lookupPreview: SUPPORTED_PREVIEW });
    renderView(LOOKUP_TABLES[1], CACHES[1], DATA_ADAPTERS[1]);

    await screen.findByText(/Lookup table test error/i);
  });

  it('should preview the lookup table', async () => {
    mockUseFetchLookupPreview.mockReturnValue({ lookupPreview: SUPPORTED_PREVIEW });
    renderView(LOOKUP_TABLES[0], CACHES[0], DATA_ADAPTERS[0]);

    await screen.findByText(/"100": "Continue"/i);
    screen.getByText(/"101": "Switching Protocols"/i);
    screen.getByText(/"203": "Non-Authoritative Information"/i);
  });

  it('should test one table key', async () => {
    mockUseFetchLookupPreview.mockReturnValue({ lookupPreview: SUPPORTED_PREVIEW });
    mockTestLookupTableKey.mockImplementation(() => Promise.resolve(TEST_KEY_RESULT));
    renderView(LOOKUP_TABLES[0], CACHES[0], DATA_ADAPTERS[0]);

    const testKeyInputs = await screen.findAllByRole('textbox', { name: /key/i });

    userEvent.type(testKeyInputs[1], '203{tab}');
    userEvent.click(await screen.findByRole('button', { name: /Look up/i }));

    await screen.findByText(/"single_value": "Non-Authoritative Information"/i);
    screen.getByText(/"string_list_value": null/i);
    screen.getByText(/"has_error": false/i);
    screen.getByText(/"ttl": 9/i);
  });

  it('should show cache details side by side', async () => {
    mockUseFetchLookupPreview.mockReturnValue({ lookupPreview: SUPPORTED_PREVIEW });
    renderView(LOOKUP_TABLES[0], CACHES[0], DATA_ADAPTERS[0]);

    userEvent.click(await screen.findByRole('link', { name: /cache details/i }));

    await screen.findByText(CACHES[0].description);
  });

  it('should show data adapter details side by side', async () => {
    mockUseFetchLookupPreview.mockReturnValue({ lookupPreview: SUPPORTED_PREVIEW });
    renderView(LOOKUP_TABLES[0], CACHES[0], DATA_ADAPTERS[0]);

    userEvent.click(await screen.findByRole('link', { name: /adapter details/i }));

    await screen.findByText(DATA_ADAPTERS[0].description);
  });
});
