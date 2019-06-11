// @flow strict
// $FlowFixMe: imports from core need to be fixed in flow
import StoreProvider from 'injection/StoreProvider';

import View from 'enterprise/logic/views/View';
import Search from 'enterprise/logic/search/Search';
import { ViewManagementActions } from './ViewManagementStore';

jest.mock('injection/StoreProvider', () => ({ getStore: jest.fn() }));
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

describe('ViewManagementStore', () => {
  it('refreshes user after creating a view', () => {
    const search = Search.create();
    const view = View.builder().newId().search(search).build();
    const CurrentUserStore = {
      reload: jest.fn(),
    };
    StoreProvider.getStore.mockReturnValue(CurrentUserStore);
    return ViewManagementActions.create(view).then(() => {
      expect(CurrentUserStore.reload).toHaveBeenCalled();
    });
  });
});
