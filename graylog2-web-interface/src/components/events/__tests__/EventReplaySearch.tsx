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
import { render, screen } from 'wrappedTestingLibrary';

import type { Event } from 'components/events/events/types';
import useViewsPlugin from 'views/test/testViewsPlugin';
import EventReplaySearch from 'components/events/EventReplaySearch';
import { events } from 'fixtures/events';

import { eventDefinition, systemEventDefinition } from './fixtures';

jest.mock('views/pages/SearchPage', () => () => <span>Embedded Search</span>);
jest.mock('components/event-definitions/replay-search/hooks/useAlertAndEventDefinitionData');
jest.mock('views/hooks/useCreateSearch', () => async (search) => search);

describe('EventDefinitionReplaySearch', () => {
  useViewsPlugin();

  // eslint-disable-next-line jest/expect-expect
  it('can replay event', async () => {
    render(
      <div>
        <EventReplaySearch
          eventDefinitionMappedData={{ eventDefinition, aggregations: [] }}
          eventData={events[0] as Event}
        />
      </div>,
    );
    await screen.findByText('Embedded Search');
  });

  it('refuses to replay system events', async () => {
    render(
      <EventReplaySearch
        eventData={events[0] as Event}
        eventDefinitionMappedData={{ eventDefinition: systemEventDefinition, aggregations: [] }}
      />,
    );
    await screen.findByText(/Event is a system event/);

    expect(screen.queryByText('Embedded Search')).not.toBeInTheDocument();
  });
});
