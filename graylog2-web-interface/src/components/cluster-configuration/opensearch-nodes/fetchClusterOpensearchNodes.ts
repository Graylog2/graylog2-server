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
import { SystemOpensearch } from '@graylog/server-api';

import type { RequestOptions } from 'routing/request';
import type { Attribute, PaginatedResponseType, SearchParams } from 'stores/PaginationTypes';

const NO_SESSION_EXTENSION: RequestOptions = { requestShouldExtendSession: false };

export type OpensearchNode = {
  id: string;
  name: string;
  version: string;
  roles: Array<string>;
  jvm_heap_max: number | null;
  jvm_heap_used_percent: number | null;
  cpu_used_percent: number | null;
  disk_used_percent: number | null;
  disk_used: number | null;
  disk_total: number | null;
};

export type OpensearchNodesResponse = {
  list: Array<OpensearchNode>;
  pagination: PaginatedResponseType;
  attributes: Array<Attribute>;
};

export const DEFAULT_OPENSEARCH_NODES_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: undefined,
};

export const fetchOpensearchNodes = async (
  params: SearchParams = DEFAULT_OPENSEARCH_NODES_SEARCH_PARAMS,
): Promise<OpensearchNodesResponse> => {
  type OpensearchNodesSort = Parameters<typeof SystemOpensearch.listNodes>[0];
  type OpensearchNodesOrder = Parameters<typeof SystemOpensearch.listNodes>[4];
  type OpensearchNodesApiResponse = Awaited<ReturnType<typeof SystemOpensearch.listNodes>>;

  const sort = (params.sort?.attributeId ?? 'name') as OpensearchNodesSort;
  const order = (params.sort?.direction ?? 'asc') as OpensearchNodesOrder;

  return SystemOpensearch.listNodes(sort, params.page, params.pageSize, params.query, order, NO_SESSION_EXTENSION).then(
    ({ attributes, pagination, elements, query }: OpensearchNodesApiResponse) => ({
      attributes,
      list: elements as Array<OpensearchNode>,
      pagination: {
        ...pagination,
        query,
      } as PaginatedResponseType,
    }),
  );
};

export const clusterOpensearchNodesKeyFn = (searchParams: SearchParams = DEFAULT_OPENSEARCH_NODES_SEARCH_PARAMS) => [
  'opensearchNodes',
  searchParams,
];
