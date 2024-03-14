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

import EventsTableRow from './EventsTableRow';

const event = {
  id: 'event-id-1',
  name: 'Event 1',
  status: null,
  assigned_to: null,
  created_at: '2024-02-26T15:32:24.666Z',
  updated_at: '2024-02-26T15:32:24.666Z',
  priority: null,
  archived: false,
  replay_info: null,
};

describe('EventsList', () => {
  it('should render list of events', async () => {
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
});
