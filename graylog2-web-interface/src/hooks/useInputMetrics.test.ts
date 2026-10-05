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

import { asMock } from 'helpers/mocking';
import fetch from 'logic/rest/FetchProvider';
import suppressConsole from 'helpers/suppressConsole';

import useInputMetrics from './useInputMetrics';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('useInputMetrics', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns metrics keyed by input id', async () => {
    asMock(fetch).mockResolvedValue({
      metrics: {
        'input-1': { extractor_count: 2, messages_per_stream: { 'stream-a': 10 } },
        'input-2': { extractor_count: 0, messages_per_stream: {} },
      },
    });

    const { result } = renderHook(() =>
      useInputMetrics(['input-1', 'input-2'], ['extractor_count', 'messages_per_stream']),
    );

    await waitFor(() => expect(result.current.isInitialLoading).toBe(false));

    expect(result.current.metricsByInputId).toEqual({
      'input-1': { extractor_count: 2, messages_per_stream: { 'stream-a': 10 } },
      'input-2': { extractor_count: 0, messages_per_stream: {} },
    });
  });

  it('requests repeated query params with sorted ids and fields', async () => {
    asMock(fetch).mockResolvedValue({ metrics: {} });

    renderHook(() => useInputMetrics(['b', 'a'], ['extractor_count', 'messages_per_stream']));

    await waitFor(() => expect(fetch).toHaveBeenCalled());

    const url = asMock(fetch).mock.calls[0][1] as string;
    expect(url).toContain('input_ids=a&input_ids=b');
    expect(url).toContain('fields=extractor_count&fields=messages_per_stream');
  });

  it('does not fetch when input ids are empty', () => {
    renderHook(() => useInputMetrics([], ['extractor_count']));

    expect(fetch).not.toHaveBeenCalled();
  });

  it('does not fetch when fields are empty', () => {
    renderHook(() => useInputMetrics(['input-1'], []));

    expect(fetch).not.toHaveBeenCalled();
  });

  it('returns an empty map and isError when the request fails', async () => {
    asMock(fetch).mockRejectedValue(new Error('boom'));

    await suppressConsole(async () => {
      const { result } = renderHook(() => useInputMetrics(['input-1'], ['extractor_count']));

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.metricsByInputId).toEqual({});
    });
  });
});
