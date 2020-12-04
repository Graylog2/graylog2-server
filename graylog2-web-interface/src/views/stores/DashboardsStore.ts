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
import Reflux from 'reflux';

import { qualifyUrl } from 'util/URLUtils';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type { RefluxActions, Store } from 'stores/StoreTypes';

import type { PaginatedViews, SortField, SortOrder } from './ViewManagementStore';

import View from '../logic/views/View';

type DashboardsActionsType = RefluxActions<{
  search: (query?: string, page?: number, perPage?: number, sortBy?: SortField, order?: SortOrder) => Promise<PaginatedViews>,
}>;

const DashboardsActions: DashboardsActionsType = singletonActions(
  'views.Dashboards',
  () => Reflux.createActions({
    search: { asyncResult: true },
  }),
);

const dashboardsUrl = qualifyUrl('/dashboards');

export type Pagination = {
  total: number;
  count: number;
  page: number;
  perPage: number;
};

export type DashboardsStoreState = {
  pagination: Pagination;
  list: Array<View> | undefined;
};

const DashboardsStore: Store<DashboardsStoreState> = singletonStore(
  'views.Dashboards',
  () => Reflux.createStore({
    listenables: [DashboardsActions],
    dashboards: undefined,
    pagination: {
      total: 0,
      count: 0,
      page: 1,
      perPage: 10,
    },

    getInitialState() {
      return {
        pagination: this.pagination,
        list: this.dashboards,
      };
    },
    search(query = '', page = 1, perPage = 10, sortBy = 'title', order = 'asc') {
      const promise = fetch('GET', `${dashboardsUrl}?query=${query}&page=${page}&per_page=${perPage}&sort=${sortBy}&order=${order}`)
        .then((response) => {
          this.dashboards = response.views;

          this.pagination = {
            total: response.total,
            count: response.count,
            page: response.page,
            perPage: response.per_page,
          };

          this.trigger({
            list: this.dashboards,
            pagination: this.pagination,
          });

          return {
            list: this.dashboards,
            pagination: this.pagination,
          };
        })
        .catch((error) => {
          UserNotification.error(`Fetching dashboards failed with status: ${error}`,
            'Could not retrieve dashboards');
        });

      DashboardsActions.search.promise(promise);
    },
  }),
);

export { DashboardsActions, DashboardsStore };
