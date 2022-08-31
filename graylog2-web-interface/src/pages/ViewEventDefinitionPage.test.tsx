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
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';

import mockAction from 'helpers/mocking/MockAction';
import MockStore from 'helpers/mocking/StoreMock';
import mockComponent from 'helpers/mocking/MockComponent';
import { simpleEventDefinition as mockEventDefinition } from 'fixtures/eventDefinition';
import { adminUser } from 'fixtures/users';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';

import ViewEventDefinitionPage from './ViewEventDefinitionPage';

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: jest.fn(() => ({
    definitionId: mockEventDefinition.id,
  })),
}));

jest.mock('hooks/useCurrentUser');

jest.mock('stores/event-definitions/EventDefinitionsStore', () => ({
  EventDefinitionsActions: {
    get: mockAction(jest.fn(() => Promise.resolve({ event_definition: mockEventDefinition, context: { scheduler: { is_scheduled: true } } }))),
  },
}));

jest.mock('stores/event-notifications/EventNotificationsStore', () => ({
  EventNotificationsActions: {
    listAll: mockAction(),
  },
  EventNotificationsStore: MockStore((['getInitialState', () => ({ all: [] })])),
}));

jest.mock('components/event-definitions/event-definition-form/EventDefinitionSummary', () => mockComponent('EventDefinitionSummary'));

describe('<ViewEventDefinitionPage />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('should display the event definition page', async () => {
    render(<ViewEventDefinitionPage />);

    await screen.findByText(/View Event Definition/);
  });

  it('should display event details when permitted', async () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
      .permissions(Immutable.List([`eventdefinitions:read:${mockEventDefinition.id}`]))
      .build());

    render(<ViewEventDefinitionPage />);

    await screen.findByText(/Event Definition 1/);
  });

  it('should display the edit button when allowed', async () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
      .permissions(Immutable.List([`eventdefinitions:read:${mockEventDefinition.id}`, `eventdefinitions:edit:${mockEventDefinition.id}`]))
      .build());

    render(<ViewEventDefinitionPage />);

    await screen.findAllByRole('link', {
      name: /edit event definition/i,
    });
  });
});
