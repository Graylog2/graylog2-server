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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { SystemInputs } from '@graylog/server-api';

import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};
export type InputSummary = {
  creator_user_id: string;
  node: string;
  name: string;
  created_at: string;
  global: boolean;
  attributes: {
    [key: string]: any;
  };
  id: string;
  title: string;
  type: string;
  content_pack: string;
  static_fields: {
    [key: string]: string;
  };
};

export const KEY_PREFIX = ['inputs', 'overview'];
export const keyFn = (searchParams: SearchParams) => [...KEY_PREFIX, searchParams];

export const fetchInputs = (searchParams: SearchParams) =>
  SystemInputs.getPage(
    searchParams.sort.attributeId as any,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    FiltersForQueryParams(searchParams.filters),
    searchParams?.sort.direction,
  ).then((response) => ({
    list: response.elements,
    attributes: response.attributes,
    pagination: { total: response.total },
  }));

type Options = {
  enabled: boolean;
};

type InputsResponse = {
  list: Array<InputSummary>;
  pagination: { total: number };
  attributes: Array<Attribute>;
};

const usePaginatedInputs = (
  searchParams: SearchParams,
  { enabled }: Options = { enabled: true },
): {
  data: InputsResponse;
  refetch: () => void;
  isLoading: boolean;
} => {
  const { data, refetch, isLoading } = useQuery({
    queryKey: keyFn(searchParams),

    queryFn: () =>
      defaultOnError<InputsResponse>(
        fetchInputs(searchParams),
        'Loading inputs failed with status',
        'Could not load inputs',
      ),
    placeholderData: keepPreviousData,
    enabled,
  });

  return {
    data: data ?? INITIAL_DATA,
    refetch,
    isLoading,
  };
};

export default usePaginatedInputs;
