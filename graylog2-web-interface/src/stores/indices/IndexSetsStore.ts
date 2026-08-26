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
import isArray from 'lodash/isArray';

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { RetentionStrategyConfig, RotationStrategyConfig } from 'components/indices/Types';
import type { DataTieringConfig, DataTieringFormValues, DataTieringStatus } from 'components/indices/data-tiering';

export type IndexSetFieldRestrictionType = { type: 'immutable' | 'hidden' };

export type IndexSetFieldRestrictions = {
  [field_name: string]: Array<IndexSetFieldRestrictionType>;
};

export type IndexSetConfig = {
  can_be_default?: boolean;
  id?: string;
  title: string;
  description: string;
  index_prefix: string;
  shards: number;
  replicas: number;
  rotation_strategy_class: string;
  rotation_strategy: RotationStrategyConfig;
  retention_strategy_class: string;
  retention_strategy: RetentionStrategyConfig;
  creation_date?: string;
  index_analyzer: string;
  index_optimization_max_num_segments: number;
  index_optimization_disabled: boolean;
  field_type_refresh_interval: number;
  field_type_profile?: string | null;
  index_template_type?: string;
  index_set_template_id?: string | null;
  writable: boolean;
  default?: boolean;
  use_legacy_rotation?: boolean;
  field_restrictions?: IndexSetFieldRestrictions;
};

export type IndexSet = IndexSetConfig & {
  data_tiering?: DataTieringConfig;
  data_tiering_status?: DataTieringStatus;
};

export type IndexSetFormValues = IndexSetConfig & { data_tiering?: DataTieringFormValues };

export type IndexSetStats = {
  documents: number;
  indices: number;
  size: number;
};

export type IndexSetsStats = {
  [key: string]: IndexSetStats;
};

export type IndexSetsResponseType = {
  total: number;
  index_sets: Array<IndexSet>;
  stats: IndexSetsStats;
};

export type IndexSetsStoreState = {
  indexSetsCount: number;
  indexSets: Array<IndexSet>;
  indexSetStats: IndexSetsStats;
  indexSet: IndexSet;
  globalIndexSetStats: IndexSetStats;
};

const _errorMessage = (error) => {
  try {
    if (isArray(error.additional.body)) {
      return error.additional.body.map(({ message, path }) => `${path ?? ''} ${message}.`).join(' ');
    }

    return error.additional.body.message;
  } catch (_e) {
    return error.message;
  }
};

export const fetchIndexSets = (stats: boolean, only_open: boolean = false): Promise<IndexSetsResponseType> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndexSetsApiController.list(stats, only_open).url));

export const fetchIndexSetsPaginated = (skip: number, limit: number, stats: boolean): Promise<IndexSetsResponseType> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndexSetsApiController.listPaginated(skip, limit, stats).url));

export const searchIndexSetsPaginated = (
  searchTerm: string,
  skip: number,
  limit: number,
  stats: boolean,
): Promise<IndexSetsResponseType> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndexSetsApiController.searchPaginated(searchTerm, skip, limit, stats).url));

export const fetchIndexSet = (indexSetId: string): Promise<IndexSet> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSetId).url));

export const createIndexSet = (indexSet: IndexSet): Promise<IndexSet> => {
  const url = qualifyUrl(ApiRoutes.IndexSetsApiController.create().url);

  return fetch('POST', url, indexSet).then(
    (response: IndexSet) => {
      UserNotification.success(`Successfully created index set '${indexSet.title}'`, 'Success');

      return response;
    },
    (error) => {
      UserNotification.error(
        `Creating index set '${indexSet.title}' failed with status: ${_errorMessage(error)}`,
        'Could not create index set.',
      );

      throw error;
    },
  );
};

export const updateIndexSet = (indexSet: IndexSet): Promise<IndexSet> => {
  const url = qualifyUrl(ApiRoutes.IndexSetsApiController.get(indexSet.id).url);

  return fetch('PUT', url, indexSet).then(
    (response: IndexSet) => {
      UserNotification.success(`Successfully updated index set '${indexSet.title}'`, 'Success');

      return response;
    },
    (error) => {
      UserNotification.error(
        `Updating index set '${indexSet.title}' failed with status: ${_errorMessage(error)}`,
        'Could not update index set.',
      );

      throw error;
    },
  );
};

export const deleteIndexSet = (indexSet: IndexSet, deleteIndices: boolean): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.IndexSetsApiController.delete(indexSet.id, deleteIndices).url);

  return fetch('DELETE', url).then(
    () => {
      UserNotification.success(`Successfully deleted index set '${indexSet.title}'`, 'Success');
    },
    (error) => {
      UserNotification.error(
        `Deleting index set '${indexSet.title}' failed with status: ${_errorMessage(error)}`,
        'Could not delete index set.',
      );

      throw error;
    },
  );
};

export const setDefaultIndexSet = (indexSet: IndexSet): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.IndexSetsApiController.setDefault(indexSet.id).url);

  return fetch('PUT', url).then(
    () => {
      UserNotification.success(`Successfully set index set '${indexSet.title}' as default`, 'Success');
    },
    (error) => {
      UserNotification.error(
        `Setting index set '${indexSet.title}' as default failed with status: ${_errorMessage(error)}`,
        'Could not set default index set.',
      );

      throw error;
    },
  );
};

export const fetchIndexSetsStats = (): Promise<IndexSetStats> =>
  fetch('GET', qualifyUrl(ApiRoutes.IndexSetsApiController.stats().url)).then((response) => ({
    indices: response.indices,
    documents: response.documents,
    size: response.size,
  }));
