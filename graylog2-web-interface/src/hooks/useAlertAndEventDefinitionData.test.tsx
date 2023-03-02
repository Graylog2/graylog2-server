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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import { useRouteMatch } from 'react-router-dom';

import { mockEventData, mockEventDefinition } from 'helpers/mocking/EventAndEventDefinitions_mock';
import suppressConsole from 'helpers/suppressConsole';
import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import Routes from 'routing/Routes';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('@tanstack/react-query', () => ({
  ...jest.requireActual('@tanstack/react-query'),
  useQueryClient: () => ({
    // setQueryData: jest.fn(() => ({ data: [{ label: 'Blue', id: 34 }] })),
    // cancelQueries: jest.fn(),
    // invalidateQueries: jest.fn(),
    ...jest.requireActual('@tanstack/react-query').useQueryClient(),
    getQueryData: jest
      .fn().mockImplementation(([key, id]) => {
        if (key === 'event-by-id' && id === mockEventData.event.id) return mockEventData.event;
        if (key === 'event-definition-by-id' && id === mockEventDefinition.id) return mockEventDefinition;

        return undefined;
      }),
  }),
}));

const eventRoute = Routes.ALERTS.replay_search(mockEventData.event.id);
console.log({ eventRoute });

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useRouteMatch: jest.fn(() => ({
    path: `/alerts/${mockEventData.event.id}/replaysearch`,
  })),
}));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});
const wrapper = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
);

describe('useEventById', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should run fetch and store mapped response', async () => {
    asMock(useRouteMatch).mockImplementation(() => ({
      path: `/alerts/${mockEventData.event.id}/replaysearch`,
    }));

    const { result, waitFor } = renderHook(() => useAlertAndEventDefinitionData(), { wrapper });

    expect(result.current.eventDefinition).toEqual(mockEventData.event);
  });
});
