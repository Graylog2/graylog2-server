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
import PropTypes from 'prop-types';
import isArray from 'lodash/isArray';

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { singletonStore, singletonActions } from 'logic/singleton';
import type { RetentionStrategyConfig, RotationStrategyConfig } from 'components/indices/Types';
import { RetentionStrategyConfigPropType, RotationStrategyConfigPropType } from 'components/indices/Types';

export const IndexSetPropType = PropTypes.shape({
  can_be_default: PropTypes.bool,
  id: PropTypes.string,
  title: PropTypes.string,
  description: PropTypes.string.isRequired,
  index_prefix: PropTypes.string.isRequired,
  shards: PropTypes.number.isRequired,
  replicas: PropTypes.number.isRequired,
  rotation_strategy_class: PropTypes.string.isRequired,
  rotation_strategy: RotationStrategyConfigPropType.isRequired,
  retention_strategy_class: PropTypes.string.isRequired,
  retention_strategy: RetentionStrategyConfigPropType.isRequired,
  creation_date: PropTypes.string,
  index_analyzer: PropTypes.string.isRequired,
  index_optimization_max_num_segments: PropTypes.number.isRequired,
  index_optimization_disabled: PropTypes.bool.isRequired,
  field_type_refresh_interval: PropTypes.number.isRequired,
  index_template_type: PropTypes.string,
  writable: PropTypes.bool.isRequired,
  default: PropTypes.bool.isRequired,
});
export type IndexSet = {
  can_be_default?: boolean,
  id?: string,
  title: string,
  description: string,
  index_prefix: string,
  shards: number,
  replicas: number,
  rotation_strategy_class: string,
  rotation_strategy: RotationStrategyConfig,
  retention_strategy_class: string,
  retention_strategy: RetentionStrategyConfig
  creation_date?: string,
  index_analyzer: string,
  index_optimization_max_num_segments: number,
  index_optimization_disabled: boolean,
  field_type_refresh_interval: number,
  index_template_type?: string,
  writable: boolean,
  default?: boolean,
};

type IndexSetStats = {
  [key: string]: {
    documents: number,
    indices: number,
    size: number,
  },
}
type IndexSetsResponseType = {
  total: number,
  index_sets: Array<IndexSet>,
  stats: IndexSetStats,
};
type IndexSetsStoreState = {
  indexSetsCount: number,
  indexSets: Array<IndexSet>,
  indexSetStats: IndexSetStats,
  indexSet: IndexSet,
}
type IndexSetsActionsType = {
  list: () => Promise<unknown>,
  listPaginated: () => Promise<unknown>,
  get: (indexSetId: string) => Promise<unknown>,
  update: (indexSet: IndexSet) => Promise<unknown>,
  create: (indexSet: IndexSet) => Promise<unknown>,
  delete: () => Promise<unknown>,
  setDefault: () => Promise<unknown>,
  stats: () => Promise<unknown>,
};
export const IndexSetsActions = singletonActions(
  'core.IndexSets',
  () => Reflux.createActions<IndexSetsActionsType>({
    list: { asyncResult: true },
    listPaginated: { asyncResult: true },
    get: { asyncResult: true },
    update: { asyncResult: true },
    create: { asyncResult: true },
    delete: { asyncResult: true },
    setDefault: { asyncResult: true },
    stats: { asyncResult: true },
  }),
);

export const IndexSetsStore = singletonStore(
  'core.IndexSets',
  () => Reflux.createStore<IndexSetsStoreState>({
    listenables: [IndexSetsActions],
    indexSetsCount: undefined,
    indexSets: undefined,
    indexSetStats: undefined,
    indexSet: undefined,

    getInitialState() {
      return {
        indexSetsCount: this.indexSetsCount,
        indexSets: this.indexSets,
        indexSetStats: this.indexSetStats,
      };
    },
    list(stats: boolean) {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.list(stats).url);
      const promise = fetch('GET', url);

      promise
        .then(
          (response: IndexSetsResponseType) => this.trigger({
            indexSetsCount: response.total,
            indexSets: response.index_sets,
            indexSetStats: response.stats,
          }),
          (error) => {
            UserNotification.error(`Fetching index sets list failed: ${error.message}`,
              'Could not retrieve index sets.');
          },
        );

      IndexSetsActions.list.promise(promise);
    },

    listPaginated(skip: number, limit: number, stats: boolean) {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.listPaginated(skip, limit, stats).url);
      const promise = fetch('GET', url);

      promise
        .then(
          (response: IndexSetsResponseType) => this.trigger({
            indexSetsCount: response.total,
            indexSets: response.index_sets,
            indexSetStats: response.stats,
          }),
          (error) => {
            UserNotification.error(`Fetching index sets list failed: ${this._errorMessage(error)}`,
              'Could not retrieve index sets.');
          },
        );

      IndexSetsActions.listPaginated.promise(promise);
    },

    get(indexSetId: string) {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSetId).url);
      const promise = fetch('GET', url);

      promise.then(
        (response: IndexSet) => {
          this.trigger({ indexSet: response });

          return response;
        },
        (error) => {
          UserNotification.error(`Fetching index set '${indexSetId}' failed with status: ${this._errorMessage(error)}`, 'Could not retrieve index set.');
        },
      );

      IndexSetsActions.get.promise(promise);
    },

    update(indexSet: IndexSet) {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSet.id).url);
      const promise = fetch('PUT', url, indexSet);

      promise.then(
        (response: IndexSet) => {
          UserNotification.success(`Successfully updated index set '${indexSet.title}'`, 'Success');
          this.trigger({ indexSet: response });

          return response;
        },
        (error) => {
          UserNotification.error(`Updating index set '${indexSet.title}' failed with status: ${this._errorMessage(error)}`, 'Could not update index set.');
        },
      );

      IndexSetsActions.update.promise(promise);
    },

    create(indexSet: IndexSet) {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.create().url);
      const promise = fetch('POST', url, indexSet);

      promise.then(
        (response: IndexSet) => {
          UserNotification.success(`Successfully created index set '${indexSet.title}'`, 'Success');
          this.trigger({ indexSet: response });

          return response;
        },
        (error) => {
          UserNotification.error(`Creating index set '${indexSet.title}' failed with status: ${this._errorMessage(error)}`, 'Could not create index set.');
        },
      );

      IndexSetsActions.create.promise(promise);
    },

    delete(indexSet: IndexSet, deleteIndices: boolean) {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.delete(indexSet.id, deleteIndices).url);
      const promise = fetch('DELETE', url);

      promise.then(
        () => {
          UserNotification.success(`Successfully deleted index set '${indexSet.title}'`, 'Success');
        },
        (error) => {
          UserNotification.error(`Deleting index set '${indexSet.title}' failed with status: ${this._errorMessage(error)}`, 'Could not delete index set.');
        },
      );

      IndexSetsActions.delete.promise(promise);
    },

    setDefault(indexSet: IndexSet) {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.setDefault(indexSet.id).url);
      const promise = fetch('PUT', url);

      promise.then(
        () => {
          UserNotification.success(`Successfully set index set '${indexSet.title}' as default`, 'Success');
        },
        (error) => {
          UserNotification.error(`Setting index set '${indexSet.title}' as default failed with status: ${this._errorMessage(error)}`, 'Could not set default index set.');
        },
      );

      IndexSetsActions.setDefault.promise(promise);
    },

    stats() {
      const url = qualifyUrl(ApiRoutes.IndexSetsApiController.stats().url);
      const promise = fetch('GET', url);

      promise
        .then(
          (response) => this.trigger({
            globalIndexSetStats: {
              indices: response.indices,
              documents: response.documents,
              size: response.size,
            },
          }),
          (error) => {
            UserNotification.error(`Fetching global index stats failed: ${error.message}`,
              'Could not retrieve global index stats.');
          },
        );

      IndexSetsActions.stats.promise(promise);
    },

    _errorMessage(error) {
      try {
        if (isArray(error.additional.body)) {
          return error.additional.body.map(({ message, path }) => `${path ?? ''} ${message}`).join(' ; ');
        }

        return error.additional.body.message;
      } catch (e) {
        return error.message;
      }
    },
  }),
);
