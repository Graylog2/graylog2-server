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
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, act, waitFor } from 'wrappedTestingLibrary/hooks';

import { Streams } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import {
  updateStream as updateStreamApi,
  removeStream as removeStreamApi,
  pauseStream as pauseStreamApi,
  resumeStream as resumeStreamApi,
  cloneStream as cloneStreamApi,
} from 'api/streams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import useStreamMutations from 'hooks/useStreamMutations';

jest.mock('@graylog/server-api', () => ({
  Streams: {
    create: jest.fn(),
  },
}));

jest.mock('api/streams', () => ({
  updateStream: jest.fn(),
  removeStream: jest.fn(),
  pauseStream: jest.fn(),
  resumeStream: jest.fn(),
  cloneStream: jest.fn(),
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
}));

jest.mock('stores/users/CurrentUserStore', () => ({
  CurrentUserStore: {
    reload: jest.fn(),
  },
}));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const wrapper = ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;

describe('useStreamMutations', () => {
  let invalidateQueriesSpy: jest.SpyInstance;

  beforeEach(() => {
    asMock(Streams.create).mockResolvedValue({ stream_id: 'new-stream-id' });
    asMock(updateStreamApi).mockResolvedValue(undefined);
    asMock(removeStreamApi).mockResolvedValue(undefined);
    asMock(pauseStreamApi).mockResolvedValue(undefined);
    asMock(resumeStreamApi).mockResolvedValue(undefined);
    asMock(cloneStreamApi).mockResolvedValue(undefined);
    invalidateQueriesSpy = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
  });

  afterEach(() => {
    invalidateQueriesSpy.mockRestore();
    jest.clearAllMocks();
  });

  it('createStream invalidates the streams list and reloads the current user', async () => {
    const { result } = renderHook(() => useStreamMutations(), { wrapper });

    await act(async () => {
      await result.current.createStream({} as never);
    });

    await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['streams'] }));
    expect(CurrentUserStore.reload).toHaveBeenCalled();
  });

  it('updateStream invalidates the streams list and the single stream', async () => {
    const { result } = renderHook(() => useStreamMutations(), { wrapper });

    await act(async () => {
      await result.current.updateStream({ streamId: 'stream-id', data: {} });
    });

    await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['streams'] }));
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['stream', 'stream-id'] });
  });

  it('removeStream invalidates the streams list and the single stream', async () => {
    const { result } = renderHook(() => useStreamMutations(), { wrapper });

    await act(async () => {
      await result.current.removeStream('stream-id');
    });

    await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['streams'] }));
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['stream', 'stream-id'] });
  });

  it('pauseStream invalidates the streams list and the single stream', async () => {
    const { result } = renderHook(() => useStreamMutations(), { wrapper });

    await act(async () => {
      await result.current.pauseStream('stream-id');
    });

    await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['streams'] }));
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['stream', 'stream-id'] });
  });

  it('resumeStream invalidates the streams list and the single stream', async () => {
    const { result } = renderHook(() => useStreamMutations(), { wrapper });

    await act(async () => {
      await result.current.resumeStream('stream-id');
    });

    await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['streams'] }));
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['stream', 'stream-id'] });
  });

  it('cloneStream invalidates the streams list', async () => {
    const { result } = renderHook(() => useStreamMutations(), { wrapper });

    await act(async () => {
      await result.current.cloneStream({ streamId: 'stream-id', data: {} as never });
    });

    await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({ queryKey: ['streams'] }));
  });
});
