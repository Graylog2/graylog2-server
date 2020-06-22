// @flow strict
import StoreProvider from 'injection/StoreProvider';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';

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
