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

import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import { SystemRuntimeTunables } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';

import useRuntimeTunables from './useRuntimeTunables';
import { rawListResponse, passwordFeatureSummary } from './__tests__/fixtures';

jest.mock('@graylog/server-api', () => ({
  SystemRuntimeTunables: { list: jest.fn(), getFeature: jest.fn(), update: jest.fn(), reset: jest.fn() },
}));
jest.mock('util/UserNotification', () => ({ error: jest.fn(), success: jest.fn() }));

describe('useRuntimeTunables', () => {
  it('returns parsed feature summaries', async () => {
    asMock(SystemRuntimeTunables.list).mockResolvedValue(rawListResponse);

    const { result } = renderHook(() => useRuntimeTunables());

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.features).toEqual([
      passwordFeatureSummary,
      { id: 'search', title: 'Search', tunableCount: 0 },
    ]);
  });

  it('returns no features on error', async () => {
    asMock(SystemRuntimeTunables.list).mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useRuntimeTunables());

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.features).toEqual([]);
  });
});
