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

export const MongodbStatus = {
  STARTUP: 0,
  PRIMARY: 1,
  SECONDARY: 2,
  RECOVERING: 3,
  STARTUP2: 5,
  UNKNOWN: 6,
  ARBITER: 7,
  DOWN: 8,
  ROLLBACK: 9,
  REMOVED: 10,
} as const;

export type MongodbStatusType = typeof MongodbStatus[keyof typeof MongodbStatus];

export type MongodbNode = {
  id: string;
  name: string;
  role: string;
  version: string;
  status: number;
  replicationLag: number;
  slowQueryCount: number | null;
  storageUsedPercent: number;
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
      elements: Array<Record<string, unknown>>;
    }) => ({
      attributes,
      list: elements.map((node) => ({
        id: node.name as string,
        name: node.name as string,
        role: node.role as string,
        version: node.version as string,
        status: node.status as number,
        replicationLag: node.replication_lag as number,
        slowQueryCount: node.slow_query_count as number | null,
        storageUsedPercent: node.storage_used_percent as number,
      })),
      pagination,
    }),
  );
};

export const clusterMongodbNodesKeyFn = (searchParams: SearchParams = DEFAULT_MONGODB_NODES_SEARCH_PARAMS) => [
  'mongodbNodes',
  searchParams,
];
