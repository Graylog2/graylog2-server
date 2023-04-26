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

import { mockEventDefinitionTwoAggregations, mockedMappedAggregation } from 'helpers/mocking/EventAndEventDefinitions_mock';
import suppressConsole from 'helpers/suppressConsole';
import UserNotification from 'util/UserNotification';
import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import useEventDefinition, { definitionsUrl } from 'hooks/useEventDefinition';

const definitionUrl = definitionsUrl(mockEventDefinitionTwoAggregations.id);

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

jest.mock('views/logic/Widgets', () => ({
  ...jest.requireActual('views/logic/Widgets'),
  widgetDefinition: () => ({
    searchTypes: () => [{
      type: 'AGGREGATION',
      typeDefinition: {},
    }],
  }),
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

describe('useEventDefinition', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should run fetch and store mapped response', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve(mockEventDefinitionTwoAggregations));
    const { result, waitFor } = renderHook(() => useEventDefinition(mockEventDefinitionTwoAggregations.id), { wrapper });

    await waitFor(() => result.current.isFetched);

    expect(fetch).toHaveBeenCalledWith('GET', definitionUrl);

    expect(result.current.data.eventDefinition).toEqual(mockEventDefinitionTwoAggregations);

    expect(result.current.data.aggregations).toEqual(mockedMappedAggregation);
  });

  it('should display notification on fail', async () => {
    await suppressConsole(async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { waitFor } = renderHook(() => useEventDefinition(mockEventDefinitionTwoAggregations.id), { wrapper });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Loading event definition failed with status: Error: Error',
        'Could not load event definition'));
    });
  });
});
