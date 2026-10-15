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

import useFeatureTunables from './useFeatureTunables';
import { rawFeatureResponse, passwordFeature } from './__tests__/fixtures';

jest.mock('@graylog/server-api', () => ({
  SystemRuntimeTunables: { list: jest.fn(), getFeature: jest.fn(), update: jest.fn(), reset: jest.fn() },
}));
jest.mock('util/UserNotification', () => ({ error: jest.fn(), success: jest.fn() }));

describe('useFeatureTunables', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches and parses the tunables of a feature', async () => {
    asMock(SystemRuntimeTunables.getFeature).mockResolvedValue(rawFeatureResponse);

    const { result } = renderHook(() => useFeatureTunables('password_policy'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(SystemRuntimeTunables.getFeature).toHaveBeenCalledWith('password_policy');
    expect(result.current.tunables).toEqual(passwordFeature.tunables);
  });

  it('does not fetch when disabled', () => {
    const { result } = renderHook(() => useFeatureTunables('search', { enabled: false }));

    expect(result.current).toEqual({ tunables: [], isLoading: false, isError: false });
    expect(SystemRuntimeTunables.getFeature).not.toHaveBeenCalled();
  });
});
