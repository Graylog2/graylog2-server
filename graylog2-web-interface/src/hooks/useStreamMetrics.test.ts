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

import { StreamsMetrics } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import suppressConsole from 'helpers/suppressConsole';

import useStreamMetrics from './useStreamMetrics';

jest.mock('@graylog/server-api', () => ({
  StreamsMetrics: {
    getMetrics: jest.fn(),
  },
}));

describe('useStreamMetrics', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns metrics keyed by stream id', async () => {
    asMock(StreamsMetrics.getMetrics).mockResolvedValue({
      metrics: {
        'stream-1': {
          message_count: 126648,
          avg_processing_time_ms: 3094.12,
          max_processing_time_ms: 155589.0,
          associated_inputs: [{ id: 'input-a', type: 'input' }],
          pipelines: ['pipeline-x'],
          routing_pipelines: [],
        },
      },
    });

    const { result } = renderHook(() => useStreamMetrics(['stream-1'], ['message_count']));

    await waitFor(() => expect(result.current.isInitialLoading).toBe(false));

    expect(result.current.metricsByStreamId).toEqual({
      'stream-1': {
        message_count: 126648,
        avg_processing_time_ms: 3094.12,
        max_processing_time_ms: 155589.0,
        associated_inputs: [{ id: 'input-a', type: 'input' }],
        pipelines: ['pipeline-x'],
        routing_pipelines: [],
      },
    });
  });

  it('requests sorted stream ids and fields from the streams metrics API', async () => {
    asMock(StreamsMetrics.getMetrics).mockResolvedValue({ metrics: {} });

    renderHook(() => useStreamMetrics(['b', 'a'], ['pipelines', 'message_count']));

    await waitFor(() => expect(StreamsMetrics.getMetrics).toHaveBeenCalled());

    expect(StreamsMetrics.getMetrics).toHaveBeenCalledWith({
      stream_ids: ['a', 'b'],
      fields: ['message_count', 'pipelines'],
    });
  });

  it('does not fetch when stream ids are empty', () => {
    renderHook(() => useStreamMetrics([], ['message_count']));

    expect(StreamsMetrics.getMetrics).not.toHaveBeenCalled();
  });

  it('does not fetch when fields are empty', () => {
    renderHook(() => useStreamMetrics(['stream-1'], []));

    expect(StreamsMetrics.getMetrics).not.toHaveBeenCalled();
  });

  it('returns an empty map and isError when the request fails', async () => {
    asMock(StreamsMetrics.getMetrics).mockRejectedValue(new Error('boom'));

    await suppressConsole(async () => {
      const { result } = renderHook(() => useStreamMetrics(['stream-1'], ['message_count']));

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.metricsByStreamId).toEqual({});
    });
  });
});
