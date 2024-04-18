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
import { render, screen } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { alice, adminUser } from 'fixtures/users';

import EventsTableRow from './EventsTableRow';

jest.mock('hooks/useCurrentUser');

const event = {
  id: 'event-id-1',
  event_definition_id: 'event-definition-id-1',
  name: 'Event 1',
  status: null,
  assigned_to: null,
  created_at: '2024-02-26T15:32:24.666Z',
  updated_at: '2024-02-26T15:32:24.666Z',
  priority: null,
  archived: false,
  replay_info: {
    timerange_start: '2024-04-09T14:29:36.644Z',
    timerange_end: '2024-04-09T14:29:39.644Z',
    query: '',
    streams: [
      '000000000000000000000001',
    ],
    filters: [],
  },
};

describe('EventsTableRow', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('should render event', async () => {
    render(
      <table>
        <tbody>
          <EventsTableRow event={event}
                          fields={Immutable.OrderedSet(['name'])} />
        </tbody>
      </table>,
    );

    await screen.findByText('Event 1');
  });

  it('should render replay search action', async () => {
    render(
      <table>
        <tbody>
          <EventsTableRow event={event}
                          fields={Immutable.OrderedSet(['name'])} />
        </tbody>
      </table>,
    );

    userEvent.click(await screen.findByRole('button', { name: /toggle event actions/i }));

    await screen.findByRole('menuitem', { name: /replay search/i });
  });

  it('should not render more action menu when user does not have required permissions', async () => {
    asMock(useCurrentUser).mockReturnValue(alice);

    render(
      <table>
        <tbody>
          <EventsTableRow event={event}
                          fields={Immutable.OrderedSet(['name'])} />
        </tbody>
      </table>,
    );

    expect(screen.queryByRole('button', { name: /toggle event actions/i })).not.toBeInTheDocument();
  });
});
