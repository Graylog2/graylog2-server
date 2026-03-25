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
import { defaultUser as mockDefaultUser } from 'defaultMockValues';

import useCurrentUser from 'hooks/useCurrentUser';
import { asMock } from 'helpers/mocking';

import FilterPreview from './FilterPreview';

let resolveSearch: (value: unknown) => void;

const mockExecuteJobResult = jest.fn();
const mockStartJob = jest.fn(() => Promise.resolve({ jobIds: ['job-1'] }));

jest.mock('hooks/useCurrentUser');
jest.mock('lodash/debounce', () => (fn: (...args: unknown[]) => unknown) => fn);
jest.mock('logic/generateId', () => () => 'mock-id');

jest.mock('views/logic/slices/createSearch', () => ({
  __esModule: true,
  default: (s: unknown) => Promise.resolve(s),
}));

jest.mock('views/components/contexts/useSearchExecutors', () => ({
  __esModule: true,
  default: () => ({
    startJob: mockStartJob,
    executeJobResult: mockExecuteJobResult,
  }),
}));

const config = {
  type: 'aggregation-v1',
  query: '*',
  query_parameters: [],
  filters: [],
  streams: [],
  group_by: [],
  _is_scheduled: true,
  series: [],
  conditions: { expression: {} },
  search_within_ms: 300000,
  execute_every_ms: 300000,
  event_limit: 10,
};

const mockSearchResult = {
  result: {
    errors: [],
    forId: () => ({
      searchTypes: {
        'mock-id': {
          messages: [
            {
              index: 'graylog_0',
              message: { timestamp: '2024-01-01T00:00:00Z', _id: 'msg-1', message: 'Test message' },
            },
          ],
        },
      },
    }),
  },
};

// The search execution chain (createSearch → startJob → executeJobResult) uses multiple .then() calls.
// Each .then() schedules a microtask, so we need to yield to the event loop to let the chain reach
// the executeJobResult mock before we can resolve it with test data.
const flushPromises = () => new Promise((resolve) => { setTimeout(resolve, 0); });

describe('FilterPreview', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(mockDefaultUser);

    mockExecuteJobResult.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSearch = resolve;
        }),
    );
  });

  it('should show loading spinner while query is in-flight', async () => {
    render(<FilterPreview config={config} />);

    await screen.findByText(/loading filter preview/i);
  });

  it('should show results after query completes', async () => {
    render(<FilterPreview config={config} />);

    await flushPromises();
    resolveSearch(mockSearchResult);

    await screen.findByText('Test message');
  });

  it('should show loading spinner again when config changes trigger a re-fetch', async () => {
    const { rerender } = render(<FilterPreview config={config} />);

    await flushPromises();
    resolveSearch(mockSearchResult);

    await screen.findByText('Test message');

    rerender(<FilterPreview config={{ ...config, query: 'new_query' }} />);

    await waitFor(() => {
      expect(screen.getByText(/loading filter preview/i)).toBeInTheDocument();
    });
  });
});
