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

import { asMock } from 'helpers/mocking';
import UserNotification from 'util/UserNotification';

import useEntityTitles from './useEntityTitles';

jest.mock('@graylog/server-api', () => ({
  SystemCatalog: {
    getTitles: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
}));

describe('useEntityTitles', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('resolves titles by id and exposes not-permitted ids', async () => {
    asMock(SystemCatalog.getTitles).mockResolvedValue({
      entities: [
        { id: 'stream-a', type: 'streams', title: 'Stream A' },
        { id: 'stream-b', type: 'streams', title: 'Stream B' },
      ],
      not_permitted_to_view: ['stream-c'],
    });

    const { result } = renderHook(() =>
      useEntityTitles([
        { id: 'stream-a', type: 'streams' },
        { id: 'stream-b', type: 'streams' },
        { id: 'stream-c', type: 'streams' },
      ]),
    );

    await waitFor(() => expect(result.current.isInitialLoading).toBe(false));

    expect(result.current.titlesById).toEqual({
      'stream-a': 'Stream A',
      'stream-b': 'Stream B',
    });
    expect(result.current.notPermittedIds.has('stream-c')).toBe(true);
  });

  it('calls SystemCatalog.getTitles with the sorted entity list', async () => {
    asMock(SystemCatalog.getTitles).mockResolvedValue({ entities: [], not_permitted_to_view: [] });

    renderHook(() => useEntityTitles([{ id: 'stream-a', type: 'streams' }]));

    await waitFor(() => expect(SystemCatalog.getTitles).toHaveBeenCalled());

    expect(SystemCatalog.getTitles).toHaveBeenCalledWith({
      entities: [{ id: 'stream-a', type: 'streams' }],
    });
  });

  it('resolves the titles of hooks rendered together with one request', async () => {
    asMock(SystemCatalog.getTitles).mockResolvedValue({
      entities: [
        { id: 'pipeline-a', type: 'pipeline_processor_pipelines', title: 'Pipeline A' },
        { id: 'pipeline-b', type: 'pipeline_processor_pipelines', title: 'Pipeline B' },
      ],
      not_permitted_to_view: [],
    });

    const { result } = renderHook(() => [
      useEntityTitles([{ id: 'pipeline-a', type: 'pipeline_processor_pipelines' }]),
      useEntityTitles([
        { id: 'pipeline-a', type: 'pipeline_processor_pipelines' },
        { id: 'pipeline-b', type: 'pipeline_processor_pipelines' },
      ]),
    ]);

    await waitFor(() => expect(result.current.every(({ isInitialLoading }) => !isInitialLoading)).toBe(true));

    expect(SystemCatalog.getTitles).toHaveBeenCalledTimes(1);
    expect(SystemCatalog.getTitles).toHaveBeenCalledWith({
      entities: [
        { id: 'pipeline-a', type: 'pipeline_processor_pipelines' },
        { id: 'pipeline-b', type: 'pipeline_processor_pipelines' },
      ],
    });
    expect(result.current[0].titlesById).toEqual({ 'pipeline-a': 'Pipeline A' });
    expect(result.current[1].titlesById).toEqual({ 'pipeline-a': 'Pipeline A', 'pipeline-b': 'Pipeline B' });
  });

  it('shows one error notification when the request of hooks rendered together fails', async () => {
    asMock(SystemCatalog.getTitles).mockRejectedValue(Object.assign(new Error('Not found'), { status: 404 }));

    const { result } = renderHook(() => [
      useEntityTitles([{ id: 'pipeline-a', type: 'pipeline_processor_pipelines' }]),
      useEntityTitles([{ id: 'pipeline-b', type: 'pipeline_processor_pipelines' }]),
    ]);

    await waitFor(() => expect(result.current.every(({ isError }) => isError)).toBe(true));

    expect(UserNotification.error).toHaveBeenCalledTimes(1);
  });

  it('does not call the API when the entities list is empty', () => {
    renderHook(() => useEntityTitles([]));

    expect(SystemCatalog.getTitles).not.toHaveBeenCalled();
  });
});
