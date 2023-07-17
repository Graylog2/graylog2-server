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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import MockStore from 'helpers/mocking/StoreMock';
import asMock from 'helpers/mocking/AsMock';
import EventInfoBar from 'components/event-definitions/replay-search/EventInfoBar';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import {
  mockedMappedAggregation,
  mockEventData,
  mockEventDefinitionTwoAggregations,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { selectHighlightingRules } from 'views/logic/slices/highlightSelectors';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import useViewsPlugin from 'views/test/testViewsPlugin';

jest.mock('stores/event-notifications/EventNotificationsStore', () => ({
  EventNotificationsActions: {
    listAll: jest.fn(async () => Promise.resolve()),
  },
  EventNotificationsStore: MockStore((['getInitialState', () => ({ all: [{ id: 'email_notification_id', title: 'Email notification' }] })])),
}));

jest.mock('hooks/useAlertAndEventDefinitionData');

jest.mock('views/logic/Widgets', () => ({
  ...jest.requireActual('views/logic/Widgets'),
  widgetDefinition: () => ({
    searchTypes: () => [{
      type: 'AGGREGATION',
      typeDefinition: {},
    }],
  }),
}));

const setMockedHookCache = ({
  eventData = mockEventData.event,
  eventDefinition = mockEventDefinitionTwoAggregations,
  aggregations = mockedMappedAggregation,
  isEvent = false,
  isEventDefinition = false,
  isAlert = false,
  alertId = mockEventData.event.id,
  definitionId = mockEventDefinitionTwoAggregations.id,
  definitionTitle = mockEventDefinitionTwoAggregations.title,
}) => asMock(useAlertAndEventDefinitionData).mockImplementation(() => ({
  eventData,
  eventDefinition,
  aggregations,
  isEvent,
  isEventDefinition,
  isAlert,
  alertId,
  definitionId,
  definitionTitle,
}));

jest.mock('views/logic/slices/highlightSelectors', () => ({
  selectHighlightingRules: jest.fn(),
}));

describe('<EventInfoBar />', () => {
  const EventInfoComponent = () => (
    <TestStoreProvider>
      <EventInfoBar />
    </TestStoreProvider>
  );

  useViewsPlugin();

  beforeAll(() => {
    asMock(selectHighlightingRules)
      .mockReturnValue([
        HighlightingRule.create('count(field1)', 500, 'greater', StaticColor.create('#fff')),
        HighlightingRule.create('count(field2)', 8000, 'less', StaticColor.create('#000')),
      ],
      );
  });

  it('Always shows fields: Priority, Execute search every, Search within, Description, Notifications, Aggregation conditions', async () => {
    setMockedHookCache({
      isEvent: true,
    });

    render(<EventInfoComponent />);

    const priority = await screen.findByTitle('Priority');
    const execution = await screen.findByTitle('Execute search every');
    const searchWithin = await screen.findByTitle('Search within');
    const description = await screen.findByTitle('Description');
    const notifications = await screen.findByTitle('Notifications');
    const aggregationConditions = await screen.findByTitle('Aggregation conditions');
    const field1Condition = await screen.findByTitle('count(field1)>500');
    const field2Condition = await screen.findByTitle('count(field2)<8000');

    expect(priority).toHaveTextContent('Normal');
    expect(execution).toHaveTextContent('1 minute');
    expect(searchWithin).toHaveTextContent('1 minute');
    expect(description).toHaveTextContent('Test description');
    expect(notifications).toHaveTextContent('Email notification');
    expect(aggregationConditions).toHaveTextContent('count(field1)>500count(field2)<8000');

    expect(field1Condition.children[0]).toHaveStyle({ backgroundColor: 'rgb(255, 255, 255)' });
    expect(field2Condition.children[0]).toHaveStyle({ backgroundColor: 'rgb(0, 0, 0)' });
  });

  it('Shows event timestamp and event definition link for event', async () => {
    setMockedHookCache({
      isEvent: true,
    });

    render(<EventInfoComponent />);

    const timestamp = await screen.findByTitle('Timestamp');
    const eventDefinition = await screen.findByTitle('Event definition');

    expect(timestamp).toHaveTextContent('2023-03-02 14:43:21');
    expect(eventDefinition).toHaveTextContent('Event Definition Title');
    expect(eventDefinition.children[0]).toHaveAttribute('href', '/alerts/definitions/event-definition-id-1');
  });

  it("Didn't Shows Event definition updated at for event definition which was updated before event", async () => {
    setMockedHookCache({
      isEvent: true,
    });

    render(<EventInfoComponent />);

    const eventDefinitionUpdated = screen.queryByTitle('Event definition updated at');

    expect(eventDefinitionUpdated).not.toBeInTheDocument();
  });

  it('Shows Event definition updated at for event definition which was updated after event', async () => {
    setMockedHookCache({
      isEvent: true,
      eventDefinition: {
        ...mockEventDefinitionTwoAggregations,
        updated_at: '2023-03-21T13:28:09.296Z',
      },
    });

    render(<EventInfoComponent />);

    const eventDefinitionUpdated = await screen.findByTitle('Event definition updated at');

    expect(eventDefinitionUpdated).toHaveTextContent('2023-03-21 14:28:09');
  });

  it('Do not shows event timestamp and event definition link for event definition', async () => {
    setMockedHookCache({
      isEventDefinition: true,
      eventData: undefined,
    });

    render(<EventInfoComponent />);

    const timestamp = screen.queryByTitle('Timestamp');
    const eventDefinition = screen.queryByTitle('Event definition');

    expect(timestamp).not.toBeInTheDocument();
    expect(eventDefinition).not.toBeInTheDocument();
  });

  it('show and hide data on button click', async () => {
    setMockedHookCache({
      isEventDefinition: true,
    });

    render(<EventInfoComponent />);

    const hideButton = await screen.findByText('Hide event definition details');
    const detailsContainer = await screen.findByTestId('info-container');

    fireEvent.click(hideButton);

    expect(detailsContainer).not.toBeInTheDocument();

    const showButton = await screen.findByText('Show event definition details');

    fireEvent.click(showButton);

    await screen.findByTestId('info-container');
  });
});
