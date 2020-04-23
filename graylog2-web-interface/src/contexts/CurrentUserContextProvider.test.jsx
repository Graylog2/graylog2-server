// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import { viewsManager } from 'fixtures/users';
import { CombinedProviderMock as MockCombinedProvider, StoreMock as MockStore } from 'helpers/mocking';
import CombinedProvider from 'injection/CombinedProvider';
import CurrentUserContext from './CurrentUserContext';
import CurrentUserProvider from './CurrentUserProvider';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

jest.mock('injection/CombinedProvider', () => new MockCombinedProvider({ CurrentUser: { CurrentUserStore: MockStore() } }));

describe('CurrentUserProvider', () => {
  afterEach(cleanup);

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
