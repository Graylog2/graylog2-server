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
import { render, screen } from 'wrappedTestingLibrary';

import EventInfoBar from 'components/event-definitions/replay-search/EventInfoBar';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import {
  mockedMappedAggregation,
  mockEventData,
  mockEventDefinition,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import asMock from 'helpers/mocking/AsMock';
import MockStore from 'helpers/mocking/StoreMock';

jest.mock('stores/event-notifications/EventNotificationsStore', () => ({
  EventNotificationsActions: {
    listAll: jest.fn(async () => Promise.resolve),
  },
  EventNotificationsStore: MockStore((['getInitialState', jest.fn(() => ({ all: [] }))])),
}));

jest.mock('hooks/useAlertAndEventDefinitionData');
const setMockedHookCache = ({
  eventData = mockEventData.event,
  eventDefinition = mockEventDefinition,
  aggregations = mockedMappedAggregation,
  isEvent = false,
  isEventDefinition = false,
  isAlert = false,
  alertId = mockEventData.event.id,
  definitionId = mockEventDefinition.id,
  definitionTitle = mockEventDefinition.title,
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

/*
const commonTitles = [
  'Priority',
  'Execute search every',
  'Search within',
  'Description',
  'Notifications',
  'Aggregation conditions'];

const timestampTitle = 'Timestamp';
const edUpdatedTitle = 'Event definition updated at';
const edTitle = 'Event definition';
*/
describe('<EventInfoBar />', () => {
  it('Shows all data for alert', async () => {
    setMockedHookCache({
      isEvent: true,
    });

    render(<EventInfoBar />);

    const priority = await screen.findByTitle('Priority');

    expect(priority).toContainHTML('Normal');
  });
});
