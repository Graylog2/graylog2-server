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
import { render, screen, act } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import { alice } from 'fixtures/users';
import CurrentUser from 'logic/users/CurrentUser';

import CurrentUserContext from './CurrentUserContext';
import CurrentUserProvider from './CurrentUserProvider';

const mockEmptyState = {};

jest.mock('logic/users/CurrentUser', () => ({
  __esModule: true,
  default: { getState: jest.fn(() => mockEmptyState), subscribe: jest.fn(() => () => {}) },
}));

jest.useFakeTimers();

describe('CurrentUserProvider', () => {
  const renderSUT = () => {
    const consume = jest.fn();

    render(
      <CurrentUserProvider>
        <CurrentUserContext.Consumer>{consume}</CurrentUserContext.Consumer>
      </CurrentUserProvider>,
      { wrapper: undefined },
    );

    return consume;
  };

  it('shows spinner while current user is loading', async () => {
    render(<CurrentUserProvider>Content</CurrentUserProvider>);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('provides current user', () => {
    asMock(CurrentUser.getState).mockReturnValue({ currentUser: alice.toJSON() });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(alice);
  });
});
