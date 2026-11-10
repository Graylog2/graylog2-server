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

import { PipelinesProcessingLoad } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';

import useProcessingLoad from './useProcessingLoad';
import type { ProcessingLoadResponse } from './types';

jest.mock('@graylog/server-api', () => ({
  PipelinesProcessingLoad: {
    processingLoad: jest.fn(),
  },
}));

const successResponse: ProcessingLoadResponse = {
  available: true,
  total_cost_microseconds_per_second: 100,
  pipelines: [{ pipeline_id: 'p1', load_percent: 50 }],
  rules: [],
  stage_rules: [],
};

describe('useProcessingLoad', () => {
  beforeEach(() => {
    asMock(PipelinesProcessingLoad.processingLoad).mockResolvedValue(successResponse);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('does not fetch when disabled', async () => {
    const { result } = renderHook(() => useProcessingLoad({ enabled: false }));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(PipelinesProcessingLoad.processingLoad).not.toHaveBeenCalled();
    expect(result.current.data).toBeUndefined();
  });

  it('fetches the processing-load endpoint when enabled and exposes the response', async () => {
    const { result } = renderHook(() => useProcessingLoad({ enabled: true }));

    await waitFor(() => expect(result.current.data).toBeDefined());

    expect(PipelinesProcessingLoad.processingLoad).toHaveBeenCalled();
    expect(result.current.data).toEqual(successResponse);
    expect(result.current.isError).toBe(false);
  });

  it('exposes isError when the fetch rejects', async () => {
    asMock(PipelinesProcessingLoad.processingLoad).mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useProcessingLoad({ enabled: true }));

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.data).toBeUndefined();
  });
});
