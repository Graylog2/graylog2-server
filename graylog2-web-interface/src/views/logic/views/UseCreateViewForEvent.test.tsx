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
import * as Immutable from 'immutable';

import {
  mockedMappedAggregation,
  mockEventData,
  mockEventDefinition,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import UseCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import View from 'views/logic/views/View';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import Search from 'views/logic/search/Search';
import { AbsoluteTimeRange } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';

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
    const eventData = mockEventData.event;
    const { result } = renderHook(() => UseCreateViewForEvent({ eventData: mockEventData.event, eventDefinition: mockEventDefinition, aggregations: mockedMappedAggregation }), { wrapper });
    const query = QueryGenerator(eventData.replay_info.streams, undefined, {
      type: 'absolute',
      from: eventData?.replay_info?.timerange_start,
      to: eventData?.replay_info?.timerange_end,
    }, {
      type: 'elasticsearch',
      query_string: eventData?.replay_info?.query || '',
    });
    const search = Search.create().toBuilder().queries([query]).build();

    // const titles =

    expect(result.current).toEqual(UpdateSearchForWidgets(View.create()
      .toBuilder()
      .newId()
      .type(View.Type.Search)
      .state({
        [query.id]: ViewState.create()
          .toBuilder()
          .titles(titles)
          .widgets(Immutable.List(widgets))
          .widgetPositions(positions)
          .build(),
      })
      .search(search)
      .build()));
  });
});
