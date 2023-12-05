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
import { useParams } from 'react-router-dom';
import type { Location } from 'react-router-dom';

import useLocation from 'routing/useLocation';
import {
  mockedMappedAggregation,
  mockEventData,
  mockEventDefinitionTwoAggregations,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import asMock from 'helpers/mocking/AsMock';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '/' })));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: jest.fn(() => ({})),
}));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => [{ type: 'aggregation', defaults: {} }]) },
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

const mockedHookData = {
  alertId: mockEventData.event.id,
  definitionId: mockEventData.event.event_definition_id,
  definitionTitle: mockEventDefinitionTwoAggregations.title,
  isAlert: true,
  isEvent: false,
  isEventDefinition: false,
  eventData: mockEventData.event,
  eventDefinition: mockEventDefinitionTwoAggregations,
  aggregations: mockedMappedAggregation,
};

const mockUseRouterForEvent = (id) => asMock(useLocation).mockImplementation(() => ({
  pathname: `/alerts/${id}/replay-search`,
} as Location));

const mockUseRouterForEventDefinition = (id) => asMock(useLocation).mockImplementation(() => ({
  pathname: `/alerts/definitions/${id}/replay-search`,
} as Location));

describe('useAlertAndEventDefinitionData', () => {
  beforeEach(() => {
    queryClient.clear();

    queryClient.setQueryData(['event-definition-by-id', mockEventDefinitionTwoAggregations.id], {
      eventDefinition: mockEventDefinitionTwoAggregations,
      aggregations: mockedMappedAggregation,
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should return expected data for alert page', async () => {
    const eventId = 'event-id-1';
    mockUseRouterForEvent(eventId);

    asMock(useParams).mockImplementation(() => ({
      alertId: mockEventData.event.id,
    }));

    queryClient.setQueryData(['event-by-id', eventId], { ...mockEventData.event, id: eventId, alert: true });
    const { result } = renderHook(() => useAlertAndEventDefinitionData(), { wrapper });

    await expect(result.current).toEqual(mockedHookData);
  });

  it('should return expected data for event page', async () => {
    const eventId = 'event-id-2';

    asMock(useParams).mockImplementation(() => ({
      alertId: eventId,
    }));

    mockUseRouterForEvent(eventId);

    queryClient.setQueryData(['event-by-id', eventId], {
      ...mockEventData.event,
      eventId,
      alert: false,
    });

    const { result } = renderHook(() => useAlertAndEventDefinitionData(), { wrapper });

    await expect(result.current).toEqual({
      ...mockedHookData,
      eventData: { ...mockEventData.event, eventId, alert: false },
      alertId: eventId,
      isAlert: false,
      isEvent: true,
    });
  });

  it('should return expected data for event definition', async () => {
    asMock(useParams).mockImplementation(() => ({
      definitionId: mockEventDefinitionTwoAggregations.id,
    }));

    mockUseRouterForEventDefinition(mockEventDefinitionTwoAggregations.id);

    const { result } = renderHook(() => useAlertAndEventDefinitionData(), { wrapper });

    await expect(result.current).toEqual({
      ...mockedHookData,
      eventData: undefined,
      alertId: undefined,
      isAlert: false,
      isEvent: false,
      isEventDefinition: true,
    });
  });
});
