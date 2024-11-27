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
import upperFirst from 'lodash/upperFirst';

import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { fetchPeriodically } from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';
import type { SidecarSummary } from 'components/sidecars/types';

type Actions = {
  list: (opts: { query: string, page: number, pageSize: number, filters?: {} }) => Promise<unknown>,
  refreshList: () => Promise<unknown>,
  setAction: (action: string, selectedCollectors: { [sidecarId: string]: string[] }) => Promise<unknown>,
}
export const SidecarsAdministrationActions = singletonActions(
  'core.SidecarsAdministration',
  () => Reflux.createActions<Actions>({
    list: { asyncResult: true },
    refreshList: { asyncResult: true },
    setAction: { asyncResult: true },
  }),
);

type StoreState = {
  pagination: {
    count: number,
    page: number,
    pageSize: number,
    total: number,
    perPage: number
  },
  sidecars: Array<SidecarSummary>,
  filters: {},
  query: string,
}
type Response = {
  sidecars: Array<SidecarSummary>,
  query: string,
  filters: {},
  pagination: {
    total: number,
    count: number,
    page: number,
    per_page: number,
  }
}
export const SidecarsAdministrationStore = singletonStore(
  'core.SidecarsAdministration',
  () => Reflux.createStore<StoreState>({
    listenables: [SidecarsAdministrationActions],
    sourceUrl: '/sidecar',
    sidecars: undefined,
    filters: undefined,
    pagination: {
      count: undefined,
      page: undefined,
      pageSize: undefined,
      total: undefined,
    },
    query: undefined,

    getInitialState() {
      return {
        sidecars: this.sidecars,
        filters: this.filters,
        query: this.query,
        pagination: this.pagination,
      };
    },

    propagateChanges() {
      this.trigger({
        sidecars: this.sidecars,
        filters: this.filters,
        query: this.query,
        pagination: this.pagination,
      });
    },

    list({ query = '', page = 1, pageSize = 50, filters }) {
      const body = {
        query: query,
        page: page,
        per_page: pageSize,
        filters: filters,
      };

      const promise = fetchPeriodically('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/administration`), body);

      promise.then(
        (response: Response) => {
          this.sidecars = response.sidecars;
          this.query = response.query;
          this.filters = response.filters;

          this.pagination = {
            total: response.pagination.total,
            count: response.pagination.count,
            page: response.pagination.page,
            pageSize: response.pagination.per_page,
          };

          this.propagateChanges();

          return response;
        },
        (error) => {
          UserNotification.error(error.status === 400 ? error.responseMessage : `Fetching Sidecars failed with status: ${error.message}`,
            'Could not retrieve Sidecars');
        },
      );

      SidecarsAdministrationActions.list.promise(promise);
    },

    refreshList() {
      this.list({ query: this.query, page: this.pagination.page, pageSize: this.pagination.pageSize, filters: this.filters });
    },

    setAction(action, collectors) {
      const sidecarIds = Object.keys(collectors);
      const formattedCollectors = sidecarIds.map((sidecarId) => ({
        sidecar_id: sidecarId,
        collector_ids: collectors[sidecarId],
      }));
      const body = {
        action: action,
        collectors: formattedCollectors,
      };

      const promise = fetchPeriodically('PUT', URLUtils.qualifyUrl(`${this.sourceUrl}/administration/action`), body);

      promise.then(
        (response) => {
          UserNotification.success('', `${upperFirst(action)} for ${formattedCollectors.length} collectors requested`);

          return response;
        },
        (error) => {
          UserNotification.error(`Requesting ${action} failed with status: ${error}`,
            `Could not ${action} collectors`);
        },
      );

      SidecarsAdministrationActions.setAction.promise(promise);
    },
  }),
);
