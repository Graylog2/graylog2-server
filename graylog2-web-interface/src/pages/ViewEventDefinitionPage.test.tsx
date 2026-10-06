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
import { defaultUser } from 'defaultMockValues';
import type { Permission } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import usePluginEntities from 'hooks/usePluginEntities';
import mockComponent from 'helpers/mocking/MockComponent';
import { simpleEventDefinition as mockEventDefinition } from 'fixtures/eventDefinition';
import { adminUser } from 'fixtures/users';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { useEventDefinitionWithContext } from 'components/event-definitions/hooks/useEventDefinitions';
import type { EventNotification } from 'components/event-notifications/hooks/useEventNotifications';

import ViewEventDefinitionPage from './ViewEventDefinitionPage';

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: jest.fn(() => ({
    definitionId: mockEventDefinition.id,
  })),
}));

jest.mock('hooks/useCurrentUser');

jest.mock('components/event-definitions/hooks/useEventDefinitions', () => ({
  ...jest.requireActual('components/event-definitions/hooks/useEventDefinitions'),
  useEventDefinitionWithContext: jest.fn(),
  copyEventDefinition: jest.fn(() => Promise.resolve({ id: 'new-id', title: 'New copy' })),
}));

jest.mock('components/event-notifications/hooks/useEventNotifications', () => ({
  ...jest.requireActual('components/event-notifications/hooks/useEventNotifications'),
  useEventNotifications: jest.fn(() => ({ data: { notifications: [] as Array<EventNotification> }, isFetched: true })),
}));

jest.mock('components/event-definitions/event-definition-form/EventDefinitionSummary', () =>
  mockComponent('EventDefinitionSummary'),
);
jest.mock('hooks/usePluginEntities');

describe('<ViewEventDefinitionPage />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useEventDefinitionWithContext).mockReturnValue({
      data: {
        eventDefinition: mockEventDefinition,
        context: { scheduler: { is_scheduled: true } },
        is_mutable: true,
      },
      isFetching: false,
    });
    asMock(usePluginEntities).mockImplementation(
      (entityKey) =>
        ({
          'licenseCheck': [(_license: string) => ({ data: { valid: false } })],
          'eventProcedures': [],
          'pageNavigation': [
            {
              description: 'Alerts',
              children: [{ description: 'Event Definitions', path: Routes.ALERTS.DEFINITIONS.LIST }],
            },
          ],
          'eventDefinitions.components.editSigmaModal': [],
        })[entityKey],
    );
  });

  it('should display the event definition page', async () => {
    render(<ViewEventDefinitionPage />);

    await screen.findByText(/View "Event Definition 1" Event Definition/);
  });

  it('should display event details when permitted', async () => {
    asMock(useCurrentUser).mockReturnValue(
      adminUser
        .toBuilder()
        .permissions(Immutable.List<Permission>([`eventdefinitions:read:${mockEventDefinition.id}`]))
        .build(),
    );

    render(<ViewEventDefinitionPage />);

    await screen.findByText(/Event Definition 1/);
  });

  it('should display the edit button when allowed', async () => {
    asMock(useCurrentUser).mockReturnValue(
      adminUser
        .toBuilder()
        .permissions(
          Immutable.List<Permission>([
            `eventdefinitions:read:${mockEventDefinition.id}`,
            `eventdefinitions:edit:${mockEventDefinition.id}`,
          ]),
        )
        .build(),
    );

    render(<ViewEventDefinitionPage />);

    await screen.findAllByRole('button', {
      name: /edit event definition/i,
    });
  });
});
