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
import { useQuery } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import type { Attribute, PaginatedResponseType, SearchParams } from 'stores/PaginationTypes';
import { defaultOnError } from 'util/conditional/onError';

export type GraylogNode = {
  _id?: string;
  id: string;
  node_id: string;
  short_node_id: string;
  transport_address: string;
  hostname?: string;
  last_seen?: string;
  is_leader: boolean;
  is_processing: boolean;
  lb_status?: string;
  lifecycle?: string;
  cluster_id?: string;
  codename?: string;
  facility?: string;
  started_at?: string;
  timezone?: string;
  version?: string;
  operating_system?: string;
  type?: string;
};

export type GraylogNodes = Array<GraylogNode>;

export type GraylogNodesResponse = {
  list: GraylogNodes;
  pagination: PaginatedResponseType;
  attributes: Array<Attribute>;
};

export type UseGraylogNodesOptions = {
  enabled: boolean;
};

const DEFAULT_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: undefined,
};

export const fetchGraylogNodes = async (
  params: SearchParams = DEFAULT_SEARCH_PARAMS,
): Promise<GraylogNodesResponse> => {
  const url = PaginationURL('/system/cluster/nodes/paginated', params.page, params.pageSize, params.query, {
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
      elements: GraylogNodes;
    }) => ({
      attributes,
      list: elements,
      pagination,
    }),
  );
};

export const keyFn = (searchParams: SearchParams = DEFAULT_SEARCH_PARAMS) => ['graylogNodes', searchParams];

const useGraylogNodes = (
  searchParams: SearchParams = DEFAULT_SEARCH_PARAMS,
  { enabled }: UseGraylogNodesOptions = { enabled: true },
  refetchInterval: number | false = 5000,
) => {
  const { data, refetch, isLoading, error } = useQuery({
    queryKey: keyFn(searchParams),
    queryFn: () =>
      defaultOnError(
        fetchGraylogNodes(searchParams),
        'Loading Graylog Nodes failed with status',
        'Could not load Graylog Nodes.',
      ),
    notifyOnChangeProps: ['data', 'error'],
    refetchInterval,
    enabled,
  });

  return {
    data: data || {
      attributes: [],
      list: [],
      pagination: {
        query: '',
        page: 1,
        per_page: 0,
        total: 0,
        count: 0,
      },
    },
    refetch,
    isLoading,
    error,
  };
};

export default useGraylogNodes;
