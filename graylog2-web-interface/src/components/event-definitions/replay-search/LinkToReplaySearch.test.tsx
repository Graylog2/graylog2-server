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
import * as Immutable from 'immutable';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { alice } from 'fixtures/users';

import LinkToReplaySearch from './LinkToReplaySearch';

jest.mock('hooks/useCurrentUser');

describe('LinkToReplaySearch', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(alice);
  });

  it('renders play button when user can read the event definition', async () => {
    asMock(useCurrentUser).mockReturnValue(
      alice.toBuilder().permissions(Immutable.List(['eventdefinitions:read:event-definition-id'])).build(),
    );

    render(<LinkToReplaySearch eventDefinitionId="event-definition-id" />);

    await screen.findByRole('link', { name: /replay search/i });
  });

  it('does not render play button when user cannot read the event definition', () => {
    render(<LinkToReplaySearch eventDefinitionId="event-definition-id" />);

    expect(screen.queryByRole('link', { name: /replay search/i })).not.toBeInTheDocument();
  });

  it('checks permission for the event definition of the event, when linking to an event', () => {
    render(<LinkToReplaySearch id="event-id" eventDefinitionId="event-definition-id" isEvent />);

    expect(screen.queryByRole('link', { name: /replay search/i })).not.toBeInTheDocument();
  });
});
