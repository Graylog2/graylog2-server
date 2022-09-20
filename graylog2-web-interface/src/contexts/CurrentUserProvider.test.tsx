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
import { render } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import { alice } from 'fixtures/users';
import { StoreMock as MockStore } from 'helpers/mocking';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';

import CurrentUserContext from './CurrentUserContext';
import CurrentUserProvider from './CurrentUserProvider';

jest.mock('stores/users/CurrentUserStore', () => ({
  CurrentUserStore: MockStore(['getInitialState', jest.fn(() => ({}))]),
}));

describe('CurrentUserProvider', () => {
  const renderSUT = () => {
    const consume = jest.fn();

    render(
      <CurrentUserProvider>
        <CurrentUserContext.Consumer>
          {consume}
        </CurrentUserContext.Consumer>
      </CurrentUserProvider>,
      { wrapper: undefined },
    );

    return consume;
  };

  it('provides no data when user store is empty', () => {
    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(undefined);
  });

  it('provides current user', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: alice.toJSON() });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(alice);
  });
});
