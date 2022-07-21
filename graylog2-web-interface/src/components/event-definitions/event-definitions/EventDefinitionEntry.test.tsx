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

import { alice } from 'fixtures/users';
import { simpleEventDefinition } from 'fixtures/eventDefinition';
import CurrentUserContext from 'contexts/CurrentUserContext';

import EventDefinitionEntry from './EventDefinitionEntry';

const exampleEventDefinition = {
  ...simpleEventDefinition,
  id: 'event-definition-id',
};

jest.mock('components/permissions/EntityShareModal', () => () => <div>EntityShareModal content</div>);

describe('EventDefinitionEntry', () => {
  const renderSUT = (grnPermissions = [], permissions = [], scope = '') => {
    const currentUser = alice.toBuilder()
      .grnPermissions(Immutable.List(grnPermissions))
      .permissions(Immutable.List(permissions))
      .build();

    exampleEventDefinition._scope = scope;

    return (
      <CurrentUserContext.Provider value={currentUser}>
        <EventDefinitionEntry onDelete={() => {}}
                              onCopy={() => {}}
                              onDisable={() => {}}
                              onEnable={() => {}}
                              context={{ scheduler: {} }}
                              eventDefinition={exampleEventDefinition} />
      </CurrentUserContext.Provider>
    );
  };

  it('allows sharing for owners', async () => {
    const grnPermissions = ['entity:own:grn::::event_definition:event-definition-id'];
    render(renderSUT(grnPermissions));

    const button = screen.getAllByRole('button', { name: /Share/ })[0];
    fireEvent.click(button);

    await screen.findByText('EntityShareModal content');
  });

  it('allows sharing for admins', async () => {
    render(renderSUT([], ['*']));

    const button = screen.getAllByRole('button', { name: /Share/ })[0];
    fireEvent.click(button);

    await screen.findByText('EntityShareModal content');
  });

  it('does not allow sharing for viewer', () => {
    const grnPermissions = ['entity:view:grn::::event_definition:event-definition-id'];
    render(renderSUT(grnPermissions));

    expect(screen.getAllByRole('button', { name: /Share/ })[1]).toHaveAttribute('disabled');
  });

  it('shows "edit" button', async () => {
    render(renderSUT([], ['*'], 'DEFAULT'));

    await waitFor(() => {
      expect(screen.getAllByTestId('edit-button')[0]).toBeVisible();
    });
  });

  it('hides "edit" button for immutable definitions', async () => {
    render(renderSUT([], ['*'], 'ILLUMINATE'));

    await waitFor(() => {
      expect(screen.queryByTestId('edit-button')).toBeNull();
    });
  });

  it('shows "delete" button', async () => {
    render(renderSUT([], ['*'], 'DEFAULT'));

    await waitFor(() => {
      expect(screen.getAllByTestId('delete-button')[0]).toBeVisible();
    });
  });

  it('hides "delete" button for immutable definitions', async () => {
    render(renderSUT([], ['*'], 'ILLUMINATE'));

    await waitFor(() => {
      expect(screen.queryByTestId('delete-button')).toBeNull();
    });
  });
});
