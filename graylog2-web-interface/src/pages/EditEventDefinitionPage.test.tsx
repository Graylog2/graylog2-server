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
import { render, screen } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import mockAction from 'helpers/mocking/MockAction';
import mockComponent from 'helpers/mocking/MockComponent';
import { simpleEventDefinition as mockEventDefinition } from 'fixtures/eventDefinition';
import { adminUser } from 'fixtures/users';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import EditEventDefinitionPage from 'pages/EditEventDefinitionPage';
import useCurrentUser from 'hooks/useCurrentUser';

type entityScope = {
  is_mutable: boolean;
};

type getPermissionsByScopeReturnType = {
  loadingScopePermissions: boolean;
  scopePermissions: entityScope;
};

const exampleEntityScopeMutable: getPermissionsByScopeReturnType = {
  loadingScopePermissions: false,
  scopePermissions: { is_mutable: true },
};

const exampleEntityScopeImmutable: getPermissionsByScopeReturnType = {
  loadingScopePermissions: false,
  scopePermissions: { is_mutable: false },
};

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: jest.fn(() => ({
    definitionId: 'event-definition-1-id',
  })),
}));

jest.mock('stores/event-definitions/EventDefinitionsStore', () => ({
  EventDefinitionsActions: {
    get: mockAction(jest.fn(() => Promise.resolve({
      event_definition: mockEventDefinition,
      context: { scheduler: { is_scheduled: true } },
    }))),
  },
}));

jest.mock('hooks/useScopePermissions', () => jest.fn());
jest.mock('hooks/useCurrentUser');
jest.mock('components/event-definitions/event-definition-form/EventDefinitionFormContainer', () => mockComponent('EventDefinitionFormContainer'));

describe('<EditEventDefinitionPage />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  it('should display the event definition to edit', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);

    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
      .permissions(Immutable.List(['eventdefinitions:edit:event-definition-1-id', 'streams:read:stream-id-1']))
      .build());

    render(<EditEventDefinitionPage />);

    await screen.findByText(/Event Definition 1/);
  });

  it('should display message component', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeImmutable);
    render(<EditEventDefinitionPage />);

    await screen.findByText(/cannot be edited/);
  });
});
