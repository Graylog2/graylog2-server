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
import UseCreateViewForEventDefinition from 'views/logic/views/UseCreateViewForEventDefinition';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import { createSearch } from 'fixtures/searches';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import EventDefinitionReplaySearchPage, { onErrorHandler } from 'views/pages/EventDefinitionReplaySearchPage';
import useEventDefinition from 'hooks/useEventDefinition';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import {
  mockedMappedAggregation,
  mockEventDefinitionTwoAggregations,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import useParams from 'routing/useParams';

const mockView = createSearch();

jest.mock('views/components/Search');
jest.mock('routing/useParams');

jest.mock('views/logic/views/Actions');
jest.mock('views/logic/views/UseCreateViewForEventDefinition');
jest.mock('views/logic/views/UseProcessHooksForView');
jest.mock('views/hooks/useCreateSearch');

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

describe('EventDefinitionReplaySearchPage', () => {
  const SimpleReplaySearchPage = () => (
    <StreamsContext.Provider value={[{ id: 'deadbeef', title: 'TestStream' }]}>
      <EventDefinitionReplaySearchPage />
    </StreamsContext.Provider>
  );

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useParams).mockReturnValue({ definitionId: mockEventDefinitionTwoAggregations.id });
    asMock(UseCreateViewForEventDefinition).mockReturnValue(Promise.resolve(mockView));
    asMock(useProcessHooksForView).mockReturnValue({ status: 'loaded', view: mockView, executionState: SearchExecutionState.empty() });
    asMock(SearchComponent).mockImplementation(() => <span>Extended Search Page</span>);

    asMock(useEventDefinition).mockImplementation(() => ({
      data: { eventDefinition: mockEventDefinitionTwoAggregations, aggregations: mockedMappedAggregation },
      isLoading: false,
      isFetched: true,
      refetch: () => {},
    }));
  });

  it('should run useEventDefinition, UseCreateViewForEvent with correct parameters', async () => {
    asMock(useAlertAndEventDefinitionData).mockImplementation(() => ({
      eventData: undefined,
      eventDefinition: mockEventDefinitionTwoAggregations,
      aggregations: mockedMappedAggregation,
      isEvent: false,
      isEventDefinition: true,
      isAlert: false,
      alertId: undefined,
      definitionId: mockEventDefinitionTwoAggregations.id,
      definitionTitle: mockEventDefinitionTwoAggregations.title,
    }));

    render(<SimpleReplaySearchPage />);

    await waitFor(() => expect(useEventDefinition).toHaveBeenCalledWith(mockEventDefinitionTwoAggregations.id, {
      onErrorHandler,
    }));

    await expect(UseCreateViewForEventDefinition).toHaveBeenCalledWith({
      eventDefinition: mockEventDefinitionTwoAggregations, aggregations: mockedMappedAggregation,
    });
  });
});
