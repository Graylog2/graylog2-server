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
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import type { Attribute, PaginatedResponseType, SearchParams } from 'stores/PaginationTypes';

export const MongodbRole = {
  PRIMARY: 'PRIMARY',
  SECONDARY: 'SECONDARY',
  ARBITER: 'ARBITER',
  STANDALONE: 'STANDALONE',
} as const;

export type MongodbRoleType = typeof MongodbRole[keyof typeof MongodbRole];

export const MongodbProfilingLevel = {
  OFF: 0,
  SLOW_OPS: 1,
  ALL: 2,
} as const;

export type MongodbProfilingLevelType = typeof MongodbProfilingLevel[keyof typeof MongodbProfilingLevel];

export type MongodbNode = {
  id: string;
  name: string;
  role: MongodbRoleType;
  version: string;
  profiling_level: MongodbProfilingLevelType;
  replication_lag: number;
  slow_query_count: number | null;
  storage_used_percent: number;
  available_connections: number | null;
  current_connections: number | null;
  connections_used_percent: number;
};

export type MongodbNodesResponse = {
  list: Array<MongodbNode>;
  pagination: PaginatedResponseType;
  attributes: Array<Attribute>;
};

export const DEFAULT_MONGODB_NODES_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: undefined,
};

export const fetchMongodbNodes = async (
  params: SearchParams = DEFAULT_MONGODB_NODES_SEARCH_PARAMS,
): Promise<MongodbNodesResponse> => {
  const url = PaginationURL('/system/cluster/mongodb', params.page, params.pageSize, params.query, {
    sort: params.sort?.attributeId,
    order: params.sort?.direction,
  });

  return fetch('GET', qualifyUrl(url)).then(
    ({
      attributes,
      pagination,
      elements,
    }: {
      attributes: Array<Attribute>;
      pagination: PaginatedResponseType;
      elements: Array<MongodbNode>;
    }) => ({
      attributes,
      list: elements.map((node) => ({ ...node, id: node.name })),
      pagination,
    }),
  );
};

export const clusterMongodbNodesKeyFn = (searchParams: SearchParams = DEFAULT_MONGODB_NODES_SEARCH_PARAMS) => [
  'mongodbNodes',
  searchParams,
];
