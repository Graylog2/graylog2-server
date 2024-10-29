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
import URI from 'urijs';
import merge from 'lodash/merge';

import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { singletonStore, singletonActions } from 'logic/singleton';
import type { Collector } from 'components/sidecars/types';

type Actions = {
  getCollector: (id: string) => Promise<Collector>,
  all: () => Promise<{ collectors: Array<Collector> }>,
  list: (opts: { query?: string, page?: number, pageSize?: number }) => Promise<unknown>,
  create: (collector: Collector) => Promise<unknown>,
  update: (collector: Collector) => Promise<unknown>,
  delete: (collector: Collector) => Promise<unknown>,
  copy: (id: string, name: string) => Promise<unknown>,
  validate: (collector: Collector) => Promise<{ errors: { name: string[] }, failed: boolean }>,
}
export const CollectorsActions = singletonActions(
  'core.Collectors',
  () => Reflux.createActions<Actions>({
    getCollector: { asyncResult: true },
    all: { asyncResult: true },
    list: { asyncResult: true },
    create: { asyncResult: true },
    update: { asyncResult: true },
    delete: { asyncResult: true },
    copy: { asyncResult: true },
    validate: { asyncResult: true },
  }),
);

type StoreState = {
  query: string | undefined,
  collectors: Array<Collector>,
  pagination: {
    page: number,
    pageSize: number,
    total: number,
  },
  total: number,
}
export const CollectorsStore = singletonStore(
  'core.Collectors',
  () => Reflux.createStore<StoreState>({
    listenables: [CollectorsActions],
    sourceUrl: '/sidecar',
    collectors: undefined,
    query: undefined,
    pagination: {
      page: undefined,
      pageSize: undefined,
      total: undefined,
    },
    total: undefined,
    paginatedCollectors: undefined,

    getInitialState() {
      return {
        collectors: this.collectors,
      };
    },

    propagateChanges() {
      this.trigger({
        collectors: this.collectors,
        paginatedCollectors: this.paginatedCollectors,
        query: this.query,
        total: this.total,
        pagination: this.pagination,
      });
    },

    getCollector(collectorId) {
      const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/collectors/${collectorId}`));

      promise.catch((error) => {
        let errorMessage = `Fetching Collector failed with status: ${error}`;

        if (error.status === 404) {
          errorMessage = `Unable to find a collector with ID <${collectorId}>, please ensure it was not deleted.`;
        }

        UserNotification.error(errorMessage, 'Could not retrieve Collector');
      });

      CollectorsActions.getCollector.promise(promise);
    },

    _fetchCollectors({ query, page, pageSize }) {
      const search = {
        query: query,
        page: page,
        per_page: pageSize,
      };

      const uri = URI(`${this.sourceUrl}/collectors/summary`).search(search).toString();

      return fetch('GET', URLUtils.qualifyUrl(uri));
    },

    all() {
      const promise = this._fetchCollectors({ pageSize: 0 });

      promise
        .then(
          (response) => {
            this.collectors = response.collectors;
            this.propagateChanges();

            return response.collectors;
          },
          (error) => {
            UserNotification.error(`Fetching collectors failed with status: ${error}`,
              'Could not retrieve collectors');
          },
        );

      CollectorsActions.all.promise(promise);
    },

    list({ query = '', page = 1, pageSize = 10 }) {
      const promise = this._fetchCollectors({ query: query, page: page, pageSize: pageSize });

      promise
        .then(
          (response) => {
            this.query = response.query;

            this.pagination = {
              page: response.pagination.page,
              pageSize: response.pagination.per_page,
              total: response.pagination.total,
            };

            this.total = response.total;
            this.paginatedCollectors = response.collectors;

            this.propagateChanges();

            return response.collectors;
          },
          (error) => {
            UserNotification.error(`Fetching collectors failed with status: ${error}`,
              'Could not retrieve collectors');
          },
        );

      CollectorsActions.list.promise(promise);
    },

    refreshList() {
      this.list({ query: this.query, page: this.pagination.page, pageSize: this.pagination.pageSize });
    },

    create(collector) {
      const promise = fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/collectors`), collector);

      promise
        .then(
          (response) => {
            UserNotification.success('', 'Collector successfully created');
            this.collectors = response.collectors;
            this.propagateChanges();

            return this.collectors;
          },
          (error) => {
            UserNotification.error(`Fetching collectors failed with status: ${error}`,
              'Could not retrieve collectors');
          },
        );

      CollectorsActions.create.promise(promise);
    },

    update(collector) {
      const promise = fetch('PUT', URLUtils.qualifyUrl(`${this.sourceUrl}/collectors/${collector.id}`), collector);

      promise
        .then(
          (response) => {
            UserNotification.success('', 'Collector successfully updated');
            this.collectors = response.collectors;
            this.propagateChanges();

            return this.collectors;
          },
          (error) => {
            UserNotification.error(`Fetching collectors failed with status: ${error}`,
              'Could not retrieve collectors');
          },
        );

      CollectorsActions.update.promise(promise);
    },

    delete(collector) {
      const url = URLUtils.qualifyUrl(`${this.sourceUrl}/collectors/${collector.id}`);
      const promise = fetch('DELETE', url);

      promise
        .then((response) => {
          UserNotification.success('', `Collector "${collector.name}" successfully deleted`);
          this.refreshList();

          return response;
        }, (error) => {
          UserNotification.error(`Deleting Collector failed: ${error.status === 400 ? error.responseMessage : error.message}`,
            `Could not delete Collector "${collector.name}"`);
        });

      CollectorsActions.delete.promise(promise);
    },

    copy(collectorId, name) {
      const url = URLUtils.qualifyUrl(`${this.sourceUrl}/collectors/${collectorId}/${name}`);
      const method = 'POST';

      const promise = fetch(method, url);

      promise
        .then((response) => {
          UserNotification.success('', `Collector "${name}" successfully copied`);
          this.refreshList();

          return response;
        }, (error) => {
          UserNotification.error(`Saving collector "${name}" failed with status: ${error.message}`,
            'Could not save Collector');
        });

      CollectorsActions.copy.promise(promise);
    },

    validate(collector: Collector) {
    // set minimum api defaults for faster validation feedback
      const payload: Partial<Collector> = {
        id: ' ',
        service_type: 'exec',
        executable_path: ' ',
        default_template: ' ',
      };

      merge(payload, collector);

      const promise = fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/collectors/validate`), payload);

      promise
        .then(
          (response) => response,
          (error) => (
            UserNotification.error(`Validating collector "${payload.name}" failed with status: ${error.message}`,
              'Could not validate collector')
          ),
        );

      CollectorsActions.validate.promise(promise);
    },
  }),
);
