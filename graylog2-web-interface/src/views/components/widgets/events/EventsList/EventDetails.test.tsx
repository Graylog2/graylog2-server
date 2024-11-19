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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser, alice } from 'fixtures/users';
import usePluginEntities from 'hooks/usePluginEntities';
import useEventById from 'hooks/useEventById';
import { mockEventData, mockEventDefinitionTwoAggregations } from 'helpers/mocking/EventAndEventDefinitions_mock';
import useEventDefinition from 'components/events/events/hooks/useEventDefinition';

import EventDetails from './EventDetails';

jest.mock('hooks/useEventById');
jest.mock('hooks/useCurrentUser');
jest.mock('hooks/usePluginEntities');
jest.mock('components/events/events/hooks/useEventDefinition');

describe('EventDetails', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useEventDefinition).mockReturnValue({ data: undefined, isFetching: false, isInitialLoading: false });

    asMock(useEventById).mockImplementation(() => ({
      data: mockEventData.event,
      isLoading: false,
      isFetched: true,
      refetch: () => {},
    }));
  });

  it('should render pluggable event details', async () => {
    asMock(usePluginEntities).mockImplementation((entityKey) => ({
      'views.components.widgets.events.detailsComponent': [{
        component: () => <div>Pluggable details component</div>,
        useCondition: () => true,
        key: 'details-component',
      }],
    }[entityKey]));

    render(<EventDetails eventId="event-id" />);

    await screen.findByText('Pluggable details component');
  });

  it('should render default event details', async () => {
    asMock(useEventDefinition).mockReturnValue({ data: mockEventDefinitionTwoAggregations, isFetching: false, isInitialLoading: false });
    render(<EventDetails eventId="event-id" />);

    await waitFor(() => expect(useEventDefinition).toHaveBeenCalledWith('event-definition-id-1', true));
    await screen.findByText('Additional Fields');
  });

  it('should not fetch event definition when user does not have required permissions', async () => {
    asMock(useCurrentUser).mockReturnValue(alice);
    render(<EventDetails eventId="event-id" />);

    await waitFor(() => expect(useEventDefinition).toHaveBeenCalledWith('event-definition-id-1', false));
    await screen.findByText('Additional Fields');
  });
});
