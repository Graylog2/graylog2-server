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
// @flow strict
import * as React from 'react';
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { viewsManager as currentUser } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';

import EventDefinitionEntry from './EventDefinitionEntry';

jest.mock('components/permissions/EntityShareModal', () => () => <div>EntityShareModal content</div>);

const exampleEventDefinition = {
  alert: false,
  config: {
    conditions: { expression: null },
    execute_every_ms: 60000,
    group_by: [],
    query: '',
    query_parameters: [],
    search_within_ms: 60000,
    series: [],
    streams: ['5fad57fde23593249ad8a6af'],
    type: 'aggregation-v1',
  },
  description: '',
  field_spec: {},
  id: 'event-definition-id',
  key_spec: [],
  notification_settings: { grace_period_ms: 0, backlog_size: 0 },
  notifications: [],
  priority: 2,
  storage: [{
    streams: ['000000000000000000000002'],
    type: 'persist-to-streams-v1',
  }],
  title: 'New example',
};

describe('EventDefinitionEntry', () => {
  const renderSUT = (grn_permissions = [], permissions = []) => (
    <CurrentUserContext.Provider value={{ ...currentUser, grn_permissions, permissions }}>
      <EventDefinitionEntry onDelete={() => {}}
                            onDisable={() => {}}
                            onEnable={() => {}}
                            context={{ scheduler: {} }}
                            eventDefinition={exampleEventDefinition} />
    </CurrentUserContext.Provider>
  );

  it('allows sharing for owners', async () => {
    const grnPermissions = ['entity:own:grn::::event_definition:event-definition-id'];
    render(renderSUT(grnPermissions));

    const button = screen.getAllByRole('button', { name: /Share/ })[0];
    fireEvent.click(button);

    await waitFor(() => expect(screen.queryByText('EntityShareModal content')).not.toBeNull());
  });

  it('allows sharing for admins', async () => {
    render(renderSUT([], ['*']));

    const button = screen.getAllByRole('button', { name: /Share/ })[0];
    fireEvent.click(button);

    await waitFor(() => expect(screen.queryByText('EntityShareModal content')).not.toBeNull());
  });

  it('does not allow sharing for viewer', async () => {
    const grnPermissions = ['entity:view:grn::::event_definition:event-definition-id'];
    render(renderSUT(grnPermissions));

    expect(screen.getAllByRole('button', { name: /Share/ })[1]).toHaveAttribute('disabled');
  });
});
