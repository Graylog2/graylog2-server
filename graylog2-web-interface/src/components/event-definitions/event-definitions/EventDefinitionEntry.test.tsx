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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import { simpleEventDefinition } from 'fixtures/eventDefinition';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import useCurrentUser from 'hooks/useCurrentUser';

import EventDefinitionEntry from './EventDefinitionEntry';

const exampleEventDefinition = {
  ...simpleEventDefinition,
  id: 'event-definition-id',
};

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

const currentUser = adminUser.toBuilder().permissions(Immutable.List([])).build();

jest.mock('components/permissions/EntityShareModal', () => () => <div>EntityShareModal content</div>);
jest.mock('hooks/useScopePermissions', () => jest.fn());
jest.mock('hooks/useCurrentUser');

describe('EventDefinitionEntry', () => {
  const renderSUT = (scope = 'DEFAULT') => {
    exampleEventDefinition._scope = scope;

    return (
      <EventDefinitionEntry onDelete={() => { }}
                            onCopy={() => { }}
                            onDisable={() => { }}
                            onEnable={() => { }}
                            context={{ scheduler: {} }}
                            eventDefinition={exampleEventDefinition} />
    );
  };

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  it('allows sharing for owners', async () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List(['entity:own:grn::::event_definition:event-definition-id']))
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);
    render(renderSUT());

    const button = screen.getAllByRole('button', { name: /Share/ })[0];
    fireEvent.click(button);

    await screen.findByText('EntityShareModal content');
  });

  it('allows sharing for admins', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);
    asMock(useCurrentUser).mockReturnValue(adminUser);
    render(renderSUT());

    const button = screen.getAllByRole('button', { name: /Share/ })[0];
    fireEvent.click(button);

    await screen.findByText('EntityShareModal content');
  });

  it('does not allow sharing for viewer', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List(['entity:view:grn::::event_definition:event-definition-id']))
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);

    render(renderSUT());

    expect(screen.getAllByRole('button', { name: /Share/ })[1]).toHaveAttribute('disabled');
  });

  it('shows "edit" button', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);

    render(renderSUT('DEFAULT'));

    await waitFor(() => {
      expect(screen.getAllByTestId('edit-button')[0]).toBeVisible();
    });
  });

  it('hides "edit" button for immutable definitions', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeImmutable);

    render(renderSUT('ILLUMINATE'));

    await waitFor(() => {
      expect(screen.queryByTestId('edit-button')).toBeNull();
    });
  });

  it('shows "delete" button', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeMutable);

    render(renderSUT('DEFAULT'));

    await waitFor(() => {
      expect(screen.getAllByTestId('delete-button')[0]).toBeVisible();
    });
  });

  it('hides "delete" button for immutable definitions', async () => {
    asMock(useGetPermissionsByScope).mockReturnValue(exampleEntityScopeImmutable);

    render(renderSUT('ILLUMINATE'));

    await waitFor(() => {
      expect(screen.queryByTestId('delete-button')).toBeNull();
    });
  });
});
