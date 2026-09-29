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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useViewsPlugin from 'views/test/testViewsPlugin';
import fetch from 'logic/rest/FetchProvider';
import type { SearchesConfig } from 'components/search/SearchConfig';

import GeneralWelcomeMetrics from './GeneralWelcomeMetrics';

jest.mock('hooks/useCurrentUser');
jest.mock('hooks/useSearchConfiguration');
jest.mock('logic/rest/FetchProvider', () => {
  (window as unknown as { fetch: () => Promise<unknown> }).fetch = () =>
    Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve({}), text: () => Promise.resolve('') });

  return { ...jest.requireActual('logic/rest/FetchProvider'), __esModule: true, default: jest.fn() };
});

const SEARCH_ID = 'welcome-search-id';

type FetchCall = [string, string, unknown?];

const callsFor = (path: RegExp) => (asMock(fetch).mock.calls as Array<FetchCall>).filter(([, url]) => path.test(url));

const searchesCreated = () => callsFor(/\/views\/search$/);
const searchesExecuted = () => callsFor(/\/views\/search\/[^/]+\/execute$/);
const fieldTypesRequested = () => callsFor(/\/views\/fields$/);

describe('Welcome page metrics requests', () => {
  useViewsPlugin();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useSearchConfiguration).mockReturnValue({ config: undefined, refresh: () => {}, isInitialLoading: false });
    asMock(fetch).mockImplementation((_method: string, url: string, body?: unknown) => {
      if (url.includes('/views/fields')) return Promise.resolve([]);

      if (url.endsWith('/views/search')) {
        const search = typeof body === 'string' ? JSON.parse(body) : body;

        return Promise.resolve({ ...(search ?? {}), id: SEARCH_ID });
      }

      return Promise.resolve({});
    });
  });

  const renderAndSettle = async () => {
    const result = render(<GeneralWelcomeMetrics />);

    await screen.findByText('Top 5 Sources');
    await waitFor(() => {
      expect(searchesExecuted().length).toBeGreaterThan(0);
    });

    return result;
  };

  it('creates and executes its search only once', async () => {
    await renderAndSettle();

    expect(searchesCreated()).toHaveLength(1);
    expect(searchesExecuted()).toHaveLength(1);
  });

  it('does not request the same field types twice', async () => {
    await renderAndSettle();

    const requestedParameters = fieldTypesRequested().map(([, , body]) => JSON.stringify(body));

    expect(requestedParameters).toHaveLength(new Set(requestedParameters).size);
  });

  it('does not recreate its search when the search configuration arrives late', async () => {
    const { rerender } = await renderAndSettle();

    asMock(useSearchConfiguration).mockReturnValue({
      config: { query_time_range_limit: 'PT0S' } as SearchesConfig,
      refresh: () => {},
      isInitialLoading: false,
    });

    rerender(<GeneralWelcomeMetrics />);

    await screen.findByText('Top 5 Sources');

    expect(searchesCreated()).toHaveLength(1);
    expect(searchesExecuted()).toHaveLength(1);
  });
});
