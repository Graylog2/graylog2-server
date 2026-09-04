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

import { SystemCatalog } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import UserNotification from 'util/UserNotification';

import useEntityIndex from './useEntityIndex';

jest.mock('@graylog/server-api', () => ({
  SystemCatalog: {
    showEntityIndex: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useEntityIndex', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should group entities by type name', async () => {
    asMock(SystemCatalog.showEntityIndex).mockResolvedValue({
      entities: [
        { id: '1', title: 'Input A', type: { name: 'input', version: '1' } },
        { id: '2', title: 'Stream B', type: { name: 'stream', version: '1' } },
        { id: '3', title: 'Input C', type: { name: 'input', version: '1' } },
      ],
    });

    const { result } = renderHook(() => useEntityIndex());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.entityIndex).toBeDefined();

    const inputIds = result.current.entityIndex.input.map((e) => e.id);

    expect(inputIds).toEqual(['1', '3']);
    expect(result.current.entityIndex.stream).toHaveLength(1);
  });

  it('should return empty groups when API returns no entities', async () => {
    asMock(SystemCatalog.showEntityIndex).mockResolvedValue({ entities: [] });

    const { result } = renderHook(() => useEntityIndex());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.entityIndex).toEqual({});
  });

  it('should show notification on error', async () => {
    asMock(SystemCatalog.showEntityIndex).mockRejectedValue(new Error('Network error'));

    await suppressConsole(async () => {
      renderHook(() => useEntityIndex());

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Loading entity index failed with status: Error: Network error',
          'Could not load entity index',
        ),
      );
    });
  });
});
