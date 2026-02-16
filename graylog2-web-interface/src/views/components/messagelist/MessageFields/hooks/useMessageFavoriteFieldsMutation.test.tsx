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

import { renderHook, act } from 'wrappedTestingLibrary';
import { waitFor } from 'wrappedTestingLibrary/hooks';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

import { FavoriteFields } from '@graylog/server-api';

import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import type { Stream } from 'logic/streams/types';
import { StreamsActions } from 'views/stores/StreamsStore';
import UserNotification from 'util/UserNotification';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import useMessageFavoriteFieldsMutation from './useMessageFavoriteFieldsMutation';

jest.mock('@graylog/server-api', () => ({
  FavoriteFields: {
    set: jest.fn(),
    add: jest.fn(),
    remove: jest.fn(),
  },
}));

jest.mock('views/stores/StreamsStore', () => ({
  StreamsActions: { refresh: jest.fn() },
  StreamsStore: MockStore(['getInitialState', () => ({ streams: [{ id: 'streamId', title: 'Stream' }] })]),
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
}));

jest.mock('views/components/messagelist/MessageFields/hooks/useSendFavoriteFieldTelemetry', () => jest.fn);

jest.mock('hooks/useCurrentUser');

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});
const wrapper = ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;

const renderTestHook = (streams: Array<Stream>, initialFavoriteFields: Array<string>) =>
  renderHook(() => useMessageFavoriteFieldsMutation(streams, initialFavoriteFields), { wrapper });
describe('useMessageFavoriteFieldsMutation', () => {
  const streams: Array<Stream> = [
    {
      id: 's1',
      title: 'S1',
      matching_type: 'AND',
      description: '',
      disabled: false,
      rules: [],
      creator_user_id: null,
      created_at: '',
      alert_receivers: [],
      favorite_fields: ['a'],
    } as unknown as Stream,
    {
      id: 's2',
      title: 'S2',
      matching_type: 'AND',
      description: '',
      disabled: false,
      rules: [],
      creator_user_id: null,
      created_at: '',
      alert_receivers: [],
      favorite_fields: [],
    } as unknown as Stream,
  ];

  beforeEach(() => {
    asMock(FavoriteFields.set).mockImplementation(() => Promise.resolve());
    asMock(FavoriteFields.remove).mockImplementation(() => Promise.resolve());
    asMock(FavoriteFields.add).mockImplementation(() => Promise.resolve());
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('saveFavoriteField: calls FavoriteFields.set with correct per-stream payload and toggles isLoading', async () => {
    const initialFavoriteFields = ['a'];
    const { result } = renderTestHook(streams, initialFavoriteFields);
    act(() => {
      result.current.saveFavoriteField(['a', 'b']);
    });
    const expectedPayload = {
      fields: {
        s1: ['a', 'b'], // stream had 'a' and newAddedFields includes 'b'
        s2: ['b'],
      },
    };
    await waitFor(() => expect(FavoriteFields.set).toHaveBeenCalledWith(expectedPayload));
    await waitFor(() => expect(StreamsActions.refresh).toHaveBeenCalled());
  });

  it('saveFavoriteField: shows error notification when FavoriteFields.set rejects', async () => {
    const initialFavoriteFields = ['a'];
    asMock(FavoriteFields.set).mockImplementation(() => Promise.reject(new Error('saveFavoriteField error')));

    const { result } = renderTestHook(streams, initialFavoriteFields);

    act(() => {
      result.current.saveFavoriteField(['a', 'b']);
    });

    await waitFor(() =>
      expect(UserNotification.error).toHaveBeenCalledWith(
        'Setting fields to favorites failed with error: Error: saveFavoriteField error',
        'Could not set fields to favorites',
      ),
    );
  });

  it('toggleField: when field is already favorite -> calls FavoriteFields.remove for all streams', async () => {
    const initialFavoriteFields = ['a'];
    const { result } = renderTestHook(streams, initialFavoriteFields);

    act(() => {
      result.current.toggleField('a');
    });

    await waitFor(() => expect(FavoriteFields.remove).toHaveBeenCalledWith({ field: 'a', stream_ids: ['s1', 's2'] }));
    await waitFor(() => expect(StreamsActions.refresh).toHaveBeenCalled());
  });

  it('toggleField: when field is not favorite -> calls FavoriteFields.add for all streams', async () => {
    const initialFavoriteFields = ['a'];
    const { result } = renderTestHook(streams, initialFavoriteFields);

    act(() => {
      result.current.toggleField('b');
    });

    await waitFor(() => expect(FavoriteFields.add).toHaveBeenCalledWith({ field: 'b', stream_ids: ['s1', 's2'] }));
    await waitFor(() => expect(StreamsActions.refresh).toHaveBeenCalled());
  });

  it('toggleField: shows error notification when FavoriteFields.add rejects', async () => {
    const initialFavoriteFields = ['a'];
    asMock(FavoriteFields.add).mockImplementation(() => Promise.reject(new Error('addField error')));

    const { result } = renderTestHook(streams, initialFavoriteFields);

    act(() => {
      result.current.toggleField('b');
    });

    await waitFor(() =>
      expect(UserNotification.error).toHaveBeenCalledWith(
        'Adding field to favorites failed with error: Error: addField error',
        'Could not add field to favorites',
      ),
    );
  });

  it('toggleField: shows error notification when FavoriteFields.remove rejects', async () => {
    const initialFavoriteFields = ['a', 'b'];
    asMock(FavoriteFields.remove).mockImplementation(() => Promise.reject(new Error('removeField error')));

    const { result } = renderTestHook(streams, initialFavoriteFields);

    act(() => {
      result.current.toggleField('a');
    });

    await waitFor(() =>
      expect(UserNotification.error).toHaveBeenCalledWith(
        'Removing field from favorites failed with error: Error: removeField error',
        'Could not remove field from favorites',
      ),
    );
  });
});
