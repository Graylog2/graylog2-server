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
import { SystemMongodb } from '@graylog/server-api';

import type { Attribute, PaginatedResponseType, SearchParams } from 'stores/PaginationTypes';

export const MongodbRole = {
  PRIMARY: 'PRIMARY',
  SECONDARY: 'SECONDARY',
  ARBITER: 'ARBITER',
  STANDALONE: 'STANDALONE',
} as const;

export type MongodbRoleType = (typeof MongodbRole)[keyof typeof MongodbRole];

export const MongodbProfilingLevel = {
  OFF: 'OFF',
  SLOW_OPS: 'SLOW_OPS',
  ALL: 'ALL',
} as const;

export type MongodbProfilingLevelType = (typeof MongodbProfilingLevel)[keyof typeof MongodbProfilingLevel];

export type MongodbNode = {
  id: string;
  name: string;
  role: MongodbRoleType;
  version: string;
  profiling_level: MongodbProfilingLevelType;
  replication_lag: number | null;
  slow_query_count: number | null;
  storage_used_percent: number | null;
  available_connections: number | null;
  current_connections: number | null;
  connections_used_percent: number | null;
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
  type MongodbNodesSort = Parameters<typeof SystemMongodb.listNodes>[0];
  type MongodbNodesOrder = Parameters<typeof SystemMongodb.listNodes>[4];
  type MongodbNodesApiResponse = Awaited<ReturnType<typeof SystemMongodb.listNodes>>;

  const sort = (params.sort?.attributeId ?? 'name') as MongodbNodesSort;
  const order = (params.sort?.direction ?? 'asc') as MongodbNodesOrder;

  return SystemMongodb.listNodes(sort, params.page, params.pageSize, params.query, order).then(
    ({ attributes, pagination, elements, query }: MongodbNodesApiResponse) => ({
      attributes,
      list: elements as Array<MongodbNode>,
      pagination: {
        ...pagination,
        query,
      } as PaginatedResponseType,
    }),
  );
};

export const clusterMongodbNodesKeyFn = (searchParams: SearchParams = DEFAULT_MONGODB_NODES_SEARCH_PARAMS) => [
  'mongodbNodes',
  searchParams,
];
