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

import { SystemIndexSetsMetrics } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import suppressConsole from 'helpers/suppressConsole';

import useIndexSetMetrics from './useIndexSetMetrics';

jest.mock('@graylog/server-api', () => ({
  SystemIndexSetsMetrics: {
    getMetrics: jest.fn(),
  },
}));

describe('useIndexSetMetrics', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns metrics keyed by index set id', async () => {
    asMock(SystemIndexSetsMetrics.getMetrics).mockResolvedValue({
      metrics: { 'index-set-1': { index_count: 72, size_bytes: 1024, deflector_health: 'Green' } },
    });

    const { result } = renderHook(() => useIndexSetMetrics(['index-set-1'], ['index_count']));

    await waitFor(() =>
      expect(result.current.metricsByIndexSetId).toEqual({
        'index-set-1': { index_count: 72, size_bytes: 1024, deflector_health: 'Green' },
      }),
    );
  });

  it('requests sorted, unique ids and fields without extending the session', async () => {
    asMock(SystemIndexSetsMetrics.getMetrics).mockResolvedValue({ metrics: {} });

    renderHook(() => useIndexSetMetrics(['b', 'a', 'b'], ['size_bytes', 'index_count']));

    await waitFor(() =>
      expect(SystemIndexSetsMetrics.getMetrics).toHaveBeenCalledWith(['a', 'b'], ['index_count', 'size_bytes'], {
        requestShouldExtendSession: false,
      }),
    );
  });

  it('keeps previous metrics and reports loading while metrics for other index sets are pending', async () => {
    asMock(SystemIndexSetsMetrics.getMetrics)
      .mockResolvedValueOnce({ metrics: { 'index-set-1': { index_count: 72 } } })
      .mockReturnValueOnce(new Promise(() => {}));

    const { result, rerender } = renderHook(({ ids }) => useIndexSetMetrics(ids, ['index_count']), {
      initialProps: { ids: ['index-set-1'] },
    });

    await waitFor(() => expect(result.current.metricsByIndexSetId).toEqual({ 'index-set-1': { index_count: 72 } }));

    expect(result.current.isLoading).toBe(false);

    rerender({ ids: ['index-set-2'] });

    await waitFor(() => expect(result.current.isLoading).toBe(true));

    expect(result.current.metricsByIndexSetId).toEqual({ 'index-set-1': { index_count: 72 } });
  });

  it('does not fetch without index set ids', () => {
    renderHook(() => useIndexSetMetrics([], ['index_count']));

    expect(SystemIndexSetsMetrics.getMetrics).not.toHaveBeenCalled();
  });

  it('does not fetch without fields', () => {
    renderHook(() => useIndexSetMetrics(['index-set-1'], []));

    expect(SystemIndexSetsMetrics.getMetrics).not.toHaveBeenCalled();
  });

  it('reports an error when the request fails', async () => {
    asMock(SystemIndexSetsMetrics.getMetrics).mockRejectedValue(new Error('boom'));

    await suppressConsole(async () => {
      const { result } = renderHook(() => useIndexSetMetrics(['index-set-1'], ['index_count']));

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.metricsByIndexSetId).toEqual({});
    });
  });
});
