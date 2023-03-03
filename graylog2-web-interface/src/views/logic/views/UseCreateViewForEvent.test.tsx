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

import {
  mockedMappedAggregation,
  mockEventData,
  mockEventDefinition,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import UseCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';

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
    const { result } = renderHook(() => UseCreateViewForEvent({ eventData: mockEventData.event, eventDefinition: mockEventDefinition, aggregations: mockedMappedAggregation }), { wrapper });

    expect(result.current).toEqual({});
  });
});
