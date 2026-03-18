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

import useDefaultInstanceFilters from './useDefaultInstanceFilters';
import { useCollectorsConfig } from './useCollectorConfig';

jest.mock('./useCollectorConfig');

describe('useDefaultInstanceFilters', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns undefined when config is not loaded', () => {
    asMock(useCollectorsConfig).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useCollectorsConfig>);

    const { result } = renderHook(() => useDefaultInstanceFilters());

    expect(result.current).toBeUndefined();
  });

  it('returns undefined when threshold is not set', () => {
    asMock(useCollectorsConfig).mockReturnValue({
      data: {},
      isLoading: false,
    } as unknown as ReturnType<typeof useCollectorsConfig>);

    const { result } = renderHook(() => useDefaultInstanceFilters());

    expect(result.current).toBeUndefined();
  });

  it('returns filter with last_seen cutoff when threshold is configured', () => {
    asMock(useCollectorsConfig).mockReturnValue({
      data: { collector_default_visibility_threshold: 'PT24H' },
      isLoading: false,
    } as unknown as ReturnType<typeof useCollectorsConfig>);

    const { result } = renderHook(() => useDefaultInstanceFilters());

    expect(result.current).toBeDefined();

    const lastSeenFilter = result.current.get('last_seen');

    expect(lastSeenFilter).toHaveLength(1);
    expect(lastSeenFilter[0]).toMatch(/^\d{4}-\d{2}-\d{2}T.*><$/);
  });
});
