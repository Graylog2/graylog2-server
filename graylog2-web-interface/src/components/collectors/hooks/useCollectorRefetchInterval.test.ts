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
import { renderHook } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';

import useCollectorRefetchInterval from './useCollectorRefetchInterval';
import { useCollectorsConfig } from './useCollectorsConfig';

import type { CollectorsConfig } from '../types';

jest.mock('./useCollectorsConfig');

describe('useCollectorRefetchInterval', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('falls back to 30s while the config is loading', () => {
    asMock(useCollectorsConfig).mockReturnValue({ data: undefined, isLoading: true });

    const { result } = renderHook(() => useCollectorRefetchInterval());

    expect(result.current).toBe(30_000);
  });

  it('falls back to 30s when the heartbeat interval is missing', () => {
    asMock(useCollectorsConfig).mockReturnValue({ data: {} as CollectorsConfig, isLoading: false });

    const { result } = renderHook(() => useCollectorRefetchInterval());

    expect(result.current).toBe(30_000);
  });

  it('derives the interval in milliseconds from the configured heartbeat interval', () => {
    asMock(useCollectorsConfig).mockReturnValue({
      data: { collector_heartbeat_interval: 'PT45S' } as CollectorsConfig,
      isLoading: false,
    });

    const { result } = renderHook(() => useCollectorRefetchInterval());

    expect(result.current).toBe(45_000);
  });
});
