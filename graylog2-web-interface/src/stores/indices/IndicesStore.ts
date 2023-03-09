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
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

export const TIME_BASED_ROTATION_STRATEGY = 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy';
export const NOOP_RETENTION_STRATEGY = 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy';
export const TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY = 'org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy';
export const ARCHIVE_RETENTION_STRATEGY = 'org.graylog.plugins.archive.indexer.retention.strategies.ArchiveRetentionStrategy';
export const TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY_TYPE = 'org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig';
export const RETENTION = 'retention';

export type IndexTimeAndTotalStats = {
  total: number,
  time_seconds: number,
};

export type IndexShardRouting = {
  id: number,
  state: string,
  active: boolean,
  primary: boolean,
  node_id: string,
  node_name: string,
  node_hostname: string,
  relocating_to: string | null,
};

export type IndexInfo = {
  index_name: string,
  primary_shards: {
    flush: IndexTimeAndTotalStats,
    get: IndexTimeAndTotalStats,
    index: IndexTimeAndTotalStats,
    merge: IndexTimeAndTotalStats,
    refresh: IndexTimeAndTotalStats,
    search_query: IndexTimeAndTotalStats,
    search_fetch: IndexTimeAndTotalStats,
    open_search_contexts: number,
    store_size_bytes: number,
    segments: number,
    documents: {
      count: number,
      deleted: number,
    },
  },
  all_shards: {
    flush: IndexTimeAndTotalStats,
    get: IndexTimeAndTotalStats,
    index: IndexTimeAndTotalStats,
    merge: IndexTimeAndTotalStats,
    refresh: IndexTimeAndTotalStats,
    search_query: IndexTimeAndTotalStats,
    search_fetch: IndexTimeAndTotalStats,
    open_search_contexts: number,
    store_size_bytes: number,
    segments: number,
    documents: {
      count: number,
      deleted: number,
    },
  },
  routing: Array<IndexShardRouting>,
  reopened: boolean,
};

export type Indices = Array<IndexInfo>

type IndicesListResponse = {
  all: {
    indices: Indices,
  },
  closed: {
    indices: Indices,
  },
};

type IndicesActionsType = {
  list: (indexSetId: string) => Promise<unknown>,
  listAll: () => Promise<unknown>,
  close: (indexName: string) => Promise<unknown>,
  delete: (indexName: string) => Promise<unknown>,
  multiple: () => Promise<unknown>,
  reopen: () => Promise<unknown>,
  subscribe: (indexName: string) => Promise<unknown>,
  unsubscribe: (indexName: string) => Promise<unknown>,
}
export const IndicesActions = singletonActions(
  'core.Indices',
  () => Reflux.createActions<IndicesActionsType>({
    list: { asyncResult: true },
    listAll: { asyncResult: true },
    close: { asyncResult: true },
    delete: { asyncResult: true },
    multiple: { asyncResult: true },
    reopen: { asyncResult: true },
    subscribe: { asyncResult: false },
    unsubscribe: { asyncResult: false },
  }),
);
export const IndicesStore = singletonStore(
  'core.Indices',
  () => Reflux.createStore({
    listenables: [IndicesActions],
    indices: undefined,
    closedIndices: undefined,
    registrations: {},

    getInitialState() {
      return { indices: this.indices, closedIndices: this.closedIndices };
    },

    list(indexSetId: string) {
      const urlList = qualifyUrl(ApiRoutes.IndicesApiController.list(indexSetId).url);
      const promise = fetch('GET', urlList).then((response: IndicesListResponse) => {
        this.indices = response.all.indices;
        this.closedIndices = response.closed.indices;
        this.trigger({ indices: this.indices, closedIndices: this.closedIndices });

        return { indices: this.indices, closedIndices: this.closedIndices };
      });

      IndicesActions.list.promise(promise);
    },

    listAll() {
      const urlList = qualifyUrl(ApiRoutes.IndicesApiController.listAll().url);
      const promise = fetch('GET', urlList).then((response: IndicesListResponse) => {
        this.indices = response.all.indices;
        this.closedIndices = response.closed.indices;
        this.trigger({ indices: this.indices, closedIndices: this.closedIndices });

        return { indices: this.indices, closedIndices: this.closedIndices };
      });

      IndicesActions.listAll.promise(promise);
    },

    multiple() {
      const indexNames = Object.keys(this.registrations);

      if (!indexNames.length) {
        return;
      }

      const urlList = qualifyUrl(ApiRoutes.IndicesApiController.multiple().url);
      const request = { indices: indexNames };
      const promise = fetch('POST', urlList, request).then((response: Indices) => {
        this.indices = [...this.indices, ...response];
        this.trigger({ indices: this.indices, closedIndices: this.closedIndices });

        return { indices: this.indices, closedIndices: this.closedIndices };
      });

      IndicesActions.multiple.promise(promise);
    },
    close(indexName: string) {
      const url = qualifyUrl(ApiRoutes.IndicesApiController.close(indexName).url);
      const promise = fetch('POST', url);

      IndicesActions.close.promise(promise);
    },
    delete(indexName: string) {
      const url = qualifyUrl(ApiRoutes.IndicesApiController.delete(indexName).url);
      const promise = fetch('DELETE', url);

      IndicesActions.delete.promise(promise);
    },
    reopen(indexName: string) {
      const url = qualifyUrl(ApiRoutes.IndicesApiController.reopen(indexName).url);
      const promise = fetch('POST', url);

      IndicesActions.reopen.promise(promise);
    },
    subscribe(indexName: string) {
      this.registrations[indexName] = this.registrations[indexName] ? this.registrations[indexName] + 1 : 1;
    },
    unsubscribe(indexName: string) {
      this.registrations[indexName] = this.registrations[indexName] > 0 ? this.registrations[indexName] - 1 : 0;

      if (this.registrations[indexName] === 0) {
        delete this.registrations[indexName];
      }
    },
  }),
);
