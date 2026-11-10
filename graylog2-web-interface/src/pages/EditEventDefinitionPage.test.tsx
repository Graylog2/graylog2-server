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
import * as Immutable from 'immutable';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import mockComponent from 'helpers/mocking/MockComponent';
import { simpleEventDefinition as mockEventDefinition } from 'fixtures/eventDefinition';
import { adminUser } from 'fixtures/users';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import usePluginEntities from 'hooks/usePluginEntities';
import type { PermissionsByScopeReturnType } from 'hooks/useScopePermissions';
import EditEventDefinitionPage from 'pages/EditEventDefinitionPage';
import useCurrentUser from 'hooks/useCurrentUser';
import type { GenericEntityType } from 'logic/lookup-tables/types';
import { getEventDefinition } from 'components/event-definitions/hooks/useEventDefinitions';
import EventDefinitionFormContainer from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';

const exampleEntityScopeMutable: PermissionsByScopeReturnType = {
  loadingScopePermissions: false,
  scopePermissions: { is_mutable: true },
  checkPermissions: (_inEntity: Partial<GenericEntityType>) => true,
};

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: jest.fn(() => ({
    definitionId: 'event-definition-1-id',
  })),
}));

jest.mock('components/event-definitions/hooks/useEventDefinitions', () => ({
  ...jest.requireActual('components/event-definitions/hooks/useEventDefinitions'),
  getEventDefinition: jest.fn(() =>
    Promise.resolve({
      eventDefinition: mockEventDefinition,
      context: { scheduler: { is_scheduled: true } },
      is_mutable: true,
    }),
  ),
}));

jest.mock('hooks/useScopePermissions', () => jest.fn());
jest.mock('hooks/useCurrentUser');
jest.mock('components/event-definitions/event-definition-form/EventDefinitionFormContainer', () =>
  jest.fn(mockComponent('EventDefinitionFormContainer')),
);
jest.mock('hooks/usePluginEntities');

describe('<EditEventDefinitionPage />', () => {
  beforeEach(() => {
    asMock(EventDefinitionFormContainer).mockClear();
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(usePluginEntities).mockImplementation(
      (entityKey) =>
        ({
          'licenseCheck': [(_license: string) => ({ data: { valid: false } })],
          'pageNavigation': [],
        })[entityKey],
    );
  });

  it('should display the event definition to edit', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);

    asMock(useCurrentUser).mockReturnValue(
      adminUser
        .toBuilder()
        .permissions(Immutable.List(['eventdefinitions:edit:event-definition-1-id', 'streams:read:stream-id-1']))
        .build(),
    );

    render(<EditEventDefinitionPage />);

    await screen.findByText(/Event Definition 1/);
  });

  it('keeps an enabled event definition scheduled when it has no scheduler job', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);
    asMock(useCurrentUser).mockReturnValue(
      adminUser
        .toBuilder()
        .permissions(Immutable.List(['eventdefinitions:edit:event-definition-1-id', 'streams:read:stream-id-1']))
        .build(),
    );
    asMock(getEventDefinition).mockResolvedValueOnce({
      eventDefinition: { ...mockEventDefinition, state: 'ENABLED' },
      context: { scheduler: { is_scheduled: false } },
      is_mutable: true,
    });

    render(<EditEventDefinitionPage />);

    await waitFor(() => expect(EventDefinitionFormContainer).toHaveBeenCalled());

    const [{ eventDefinition }] = asMock(EventDefinitionFormContainer).mock.lastCall;

    expect(eventDefinition.config._is_scheduled).toBe(true);
  });

  it('keeps a disabled event definition unscheduled', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);
    asMock(useCurrentUser).mockReturnValue(
      adminUser
        .toBuilder()
        .permissions(Immutable.List(['eventdefinitions:edit:event-definition-1-id', 'streams:read:stream-id-1']))
        .build(),
    );
    asMock(getEventDefinition).mockResolvedValueOnce({
      eventDefinition: { ...mockEventDefinition, state: 'DISABLED' },
      context: { scheduler: { is_scheduled: true } },
      is_mutable: true,
    });

    render(<EditEventDefinitionPage />);

    await waitFor(() => expect(EventDefinitionFormContainer).toHaveBeenCalled());

    const [{ eventDefinition }] = asMock(EventDefinitionFormContainer).mock.lastCall;

    expect(eventDefinition.config._is_scheduled).toBe(false);
  });
});
