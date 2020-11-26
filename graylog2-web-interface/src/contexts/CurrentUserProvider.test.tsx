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
import { viewsManager } from 'fixtures/users';
import { CombinedProviderMock as MockCombinedProvider, StoreMock as MockStore } from 'helpers/mocking';

import CombinedProvider from 'injection/CombinedProvider';

import CurrentUserContext from './CurrentUserContext';
import CurrentUserProvider from './CurrentUserProvider';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

jest.mock('injection/CombinedProvider', () => new MockCombinedProvider({ CurrentUser: { CurrentUserStore: MockStore() } }));

describe('CurrentUserProvider', () => {
  const renderSUT = () => {
    const consume = jest.fn();

    render(
      <CurrentUserProvider>
        <CurrentUserContext.Consumer>
          {consume}
        </CurrentUserContext.Consumer>
      </CurrentUserProvider>,
    );

    return consume;
  };

  it('provides no data when user store is empty', () => {
    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(undefined);
  });

  it('provides current user', () => {
    asMock(CurrentUserStore.getInitialState).mockReturnValue({ currentUser: viewsManager });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(viewsManager);
  });
});
