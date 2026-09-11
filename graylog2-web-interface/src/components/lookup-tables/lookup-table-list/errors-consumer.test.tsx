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
import { act, render } from 'wrappedTestingLibrary';

import { ErrorsProvider } from 'components/lookup-tables/contexts/ErrorsContext';
import { ERROR_STATE } from 'components/lookup-tables/fixtures';

import ErrorsConsumer from './errors-consumer';

jest.useFakeTimers();

const mockFetchErrors = jest.fn(async () => Promise.resolve({ ...ERROR_STATE }));

jest.mock('components/lookup-tables/hooks/useLookupTablesAPI', () => ({
  __esModule: true,
  useFetchErrors: () => ({ fetchErrors: mockFetchErrors }),
}));

describe('ErrorsConsumer', () => {
  beforeEach(() => {
    mockFetchErrors.mockClear();
  });

  const advance = async (ms: number) => {
    await act(async () => {
      jest.advanceTimersByTime(ms);
      await Promise.resolve();
    });
  };

  it('polls for errors on an interval', async () => {
    render(
      <ErrorsProvider>
        <ErrorsConsumer lutNames={['table-1']} />
      </ErrorsProvider>,
    );

    await advance(1000);
    expect(mockFetchErrors).toHaveBeenCalledTimes(1);
    expect(mockFetchErrors).toHaveBeenLastCalledWith({
      lutNames: ['table-1'],
      cacheNames: undefined,
      adapterNames: undefined,
    });

    await advance(1000);
    expect(mockFetchErrors).toHaveBeenCalledTimes(2);
  });

  it('clears the polling interval on unmount instead of leaking it', async () => {
    const { unmount } = render(
      <ErrorsProvider>
        <ErrorsConsumer lutNames={['deleted-table']} />
      </ErrorsProvider>,
    );

    await advance(1000);
    expect(mockFetchErrors).toHaveBeenCalledTimes(1);

    unmount();

    await advance(5000);
    // No further polling should happen once the component has unmounted, e.g. after
    // navigating away from the page that rendered this consumer.
    expect(mockFetchErrors).toHaveBeenCalledTimes(1);
  });

  it('clears the previous interval when the watched names change instead of stacking pollers', async () => {
    const { rerender } = render(
      <ErrorsProvider>
        <ErrorsConsumer lutNames={['table-1']} />
      </ErrorsProvider>,
    );

    await advance(1000);
    expect(mockFetchErrors).toHaveBeenCalledTimes(1);

    rerender(
      <ErrorsProvider>
        <ErrorsConsumer lutNames={['table-2']} />
      </ErrorsProvider>,
    );

    await advance(1000);
    // A single poll for the new names, not two (one leaked for the old names, one for the new).
    expect(mockFetchErrors).toHaveBeenCalledTimes(2);
    expect(mockFetchErrors).toHaveBeenLastCalledWith({
      lutNames: ['table-2'],
      cacheNames: undefined,
      adapterNames: undefined,
    });
  });
});
