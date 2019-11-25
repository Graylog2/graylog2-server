// @flow strict
import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type { RefluxActions } from 'stores/StoreTypes';

import type { PaginatedViews, SortField, SortOrder } from './ViewManagementStore';
import View from '../logic/views/View';

export type SavedSearchesActionsType = RefluxActions<{
  search: (?string, ?number, ?number, ?SortField, ?SortOrder) => Promise<PaginatedViews>,
}>;

export type SavedSearchesState = {
  list: ?Array<View>,
  pagination: {
    total: number,
    page: number,
    perPage: number,
    count: number,
  },
};

const SavedSearchesActions: SavedSearchesActionsType = singletonActions(
  'views.SavedSearches',
  () => Reflux.createActions({
    search: { asyncResult: true },
  }),
);

const savedSearchesUrl = URLUtils.qualifyUrl('/views/savedSearches');

const SavedSearchesStore = singletonStore(
  'views.SavedSearches',
  () => Reflux.createStore({
    listenables: [SavedSearchesActions],
    searches: undefined,
    pagination: {
      total: 0,
      count: 0,
      page: 1,
      perPage: 10,
    },

    getInitialState() {
      return {
        pagination: this.pagination,
        list: this.searches,
      };
    },

    search(query = '', page = 1, perPage = 10, sortBy = 'title', order = 'asc') {
      const promise = fetch('GET', `${savedSearchesUrl}?query=${query}&page=${page}&per_page=${perPage}&sort=${sortBy}&order=${order}`)
        .then((response) => {
          this.searches = response.views;
          this.pagination = {
            total: response.total,
            count: response.count,
            page: response.page,
            perPage: response.per_page,
          };
          this.trigger({
            list: this.searches,
            pagination: this.pagination,
          });

          return response;
        })
        .catch((error) => {
          UserNotification.error(`Fetching saved searches failed with status: ${error}`,
            'Could not retrieve saved searches');
        });
      SavedSearchesActions.search.promise(promise);
    },
  }),
);

export { SavedSearchesActions, SavedSearchesStore };
