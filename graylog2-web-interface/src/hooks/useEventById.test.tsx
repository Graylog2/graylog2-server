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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';

import { Events } from '@graylog/server-api';

import { mockEventData } from 'helpers/mocking/EventAndEventDefinitions_mock';
import suppressConsole from 'helpers/suppressConsole';
import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import useEventById from 'hooks/useEventById';

jest.mock('@graylog/server-api', () => ({ Events: { getById: jest.fn(() => Promise.resolve(mockEventData)) } }));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});
const wrapper = ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;

jest.mock('views/logic/Widgets', () => ({
  ...jest.requireActual('views/logic/Widgets'),
  widgetDefinition: () => ({
    searchTypes: () => [
      {
        type: 'AGGREGATION',
        typeDefinition: {},
      },
    ],
  }),
}));

describe('useEventById', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should run fetch and store mapped response', async () => {
    const { result } = renderHook(() => useEventById('event-id-1'), { wrapper });

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(Events.getById).toHaveBeenCalledWith('event-id-1');
    expect(result.current.data).toEqual(mockEventData.event);
  });

  it('should display notification on fail', async () => {
    asMock(Events.getById).mockRejectedValueOnce(new Error('Error'));

    renderHook(() => useEventById('event-id-1'), { wrapper });

    await suppressConsole(async () => {
      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Loading event or alert failed with status: Error: Error',
          'Could not load event or alert',
        ),
      );
    });
  });
});
