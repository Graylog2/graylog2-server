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
import * as React from 'react';
import { render, waitFor } from 'wrappedTestingLibrary';

import MockStore from 'helpers/mocking/StoreMock';
import asMock from 'helpers/mocking/AsMock';
import SearchComponent from 'views/components/Search';
import StreamsContext from 'contexts/StreamsContext';
import UseCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import { createSearch } from 'fixtures/searches';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import EventReplaySearchPage, { onErrorHandler } from 'views/pages/EventReplaySearchPage';
import useEventById from 'hooks/useEventById';
import useEventDefinition from 'hooks/useEventDefinition';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import {
  mockedMappedAggregation,
  mockEventData,
  mockEventDefinitionTwoAggregations,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import useParams from 'routing/useParams';

const mockView = createSearch();

jest.mock('views/components/Search');
jest.mock('routing/useParams');

jest.mock('views/logic/views/Actions');
jest.mock('views/logic/views/UseCreateViewForEvent');
jest.mock('views/logic/views/UseProcessHooksForView');
jest.mock('views/hooks/useCreateSearch');
jest.mock('hooks/useEventById');
jest.mock('hooks/useEventDefinition');
jest.mock('hooks/useAlertAndEventDefinitionData');

jest.mock('stores/event-notifications/EventNotificationsStore', () => ({
  EventNotificationsActions: {
    listAll: jest.fn(async () => Promise.resolve()),
  },
  EventNotificationsStore: MockStore((['getInitialState', () => ({ all: [] })])),
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

describe('EventReplaySearchPage', () => {
  const SimpleReplaySearchPage = () => (
    <StreamsContext.Provider value={[{ id: 'deadbeef', title: 'Teststream' }]}>
      <EventReplaySearchPage />
    </StreamsContext.Provider>
  );

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useParams).mockReturnValue({ alertId: mockEventData.event.id });
    asMock(UseCreateViewForEvent).mockReturnValue(Promise.resolve(mockView));
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loaded', view: mockView, executionState: SearchExecutionState.empty() });
    asMock(SearchComponent).mockImplementation(() => <span>Extended Search Page</span>);

    asMock(useEventById).mockImplementation(() => ({
      data: mockEventData.event,
      isLoading: false,
      isFetched: true,
      refetch: () => {},
    }));

    asMock(useEventDefinition).mockImplementation(() => ({
      data: { eventDefinition: mockEventDefinitionTwoAggregations, aggregations: mockedMappedAggregation },
      isLoading: false,
      isFetched: true,
      refetch: () => {},
    }));
  });

  it('should run useEventById, useEventDefinition, UseCreateViewForEvent with correct parameters', async () => {
    asMock(useAlertAndEventDefinitionData).mockImplementation(() => ({
      eventData: mockEventData.event,
      eventDefinition: mockEventDefinitionTwoAggregations,
      aggregations: mockedMappedAggregation,
      isEvent: true,
      isEventDefinition: false,
      isAlert: false,
      alertId: mockEventData.event.id,
      definitionId: mockEventDefinitionTwoAggregations.id,
      definitionTitle: mockEventDefinitionTwoAggregations.title,
    }));

    render(<SimpleReplaySearchPage />);
    await waitFor(() => expect(useEventById).toHaveBeenCalledWith(mockEventData.event.id, { onErrorHandler }));
    await waitFor(() => expect(useEventDefinition).toHaveBeenCalledWith(mockEventData.event.event_definition_id));

    await expect(UseCreateViewForEvent).toHaveBeenCalledWith({
      eventData: mockEventData.event, eventDefinition: mockEventDefinitionTwoAggregations, aggregations: mockedMappedAggregation,
    });
  });
});
