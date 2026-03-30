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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { waitFor } from 'wrappedTestingLibrary';

import { IndexerFailures } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import UserNotification from 'util/UserNotification';

import useIndexerFailuresList from './useIndexerFailuresList';

jest.mock('@graylog/server-api', () => ({
  IndexerFailures: {
    single: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

const mockFailures = {
  total: 25,
  failures: [
    { letter_id: '1', type: 'indexing', message: 'mapping error', timestamp: '2024-01-01T00:00:00Z' },
    { letter_id: '2', type: 'indexing', message: 'parse error', timestamp: '2024-01-02T00:00:00Z' },
  ],
};

describe('useIndexerFailuresList', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should fetch failures with limit and offset', async () => {
    asMock(IndexerFailures.single).mockResolvedValue(mockFailures);

    const { result } = renderHook(() => useIndexerFailuresList(10, 0));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(IndexerFailures.single).toHaveBeenCalledWith(10, 0);
    expect(result.current.data).toEqual(mockFailures);
  });

  it('should refetch when pagination changes', async () => {
    asMock(IndexerFailures.single).mockResolvedValue(mockFailures);

    const { result, rerender } = renderHook(({ limit, offset }) => useIndexerFailuresList(limit, offset), {
      initialProps: { limit: 10, offset: 0 },
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(IndexerFailures.single).toHaveBeenCalledWith(10, 0);

    rerender({ limit: 10, offset: 10 });

    await waitFor(() => expect(IndexerFailures.single).toHaveBeenCalledWith(10, 10));
  });

  it('should show notification on error', async () => {
    asMock(IndexerFailures.single).mockRejectedValue(new Error('Server error'));

    await suppressConsole(async () => {
      renderHook(() => useIndexerFailuresList(10, 0));

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Loading indexer failures list failed with status: Error: Server error',
          'Could not load indexer failures list',
        ),
      );
    });
  });
});
