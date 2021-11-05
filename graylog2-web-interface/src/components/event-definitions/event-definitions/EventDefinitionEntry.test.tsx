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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

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
  const renderSUT = (grnPermissions = [], permissions = []) => {
    const currentUser = alice.toBuilder()
      .grnPermissions(Immutable.List(grnPermissions))
      .permissions(Immutable.List(permissions))
      .build();

    return (
      <CurrentUserContext.Provider value={currentUser}>
        <EventDefinitionEntry onDelete={() => {}}
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
});
