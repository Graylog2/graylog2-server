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

import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import { defaultOnError } from 'util/conditional/onError';
import type { InputStreamRule } from 'components/inputs/InputDiagnosis/types';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

type SortType = 'stream' | 'rule_field' | 'rule_type' | 'rule_value';

export const INPUT_STREAM_RULES_QUERY_KEY = 'input_stream_rules';

export const keyFn = (inputId: string, searchParams: SearchParams) => [
  INPUT_STREAM_RULES_QUERY_KEY,
  inputId,
  searchParams,
];

export type PaginatedInputStreamRulesResponse = {
  list: Readonly<Array<InputStreamRule>>;
  pagination: { total: number };
  attributes: Array<Attribute>;
};

export const fetchInputStreamRules = async (
  inputId: string,
  searchParams: SearchParams,
): Promise<PaginatedResponse<InputStreamRule>> =>
  SystemInputs.getStreamRulesPage(
    inputId,
    searchParams.sort.attributeId as SortType,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    FiltersForQueryParams(searchParams.filters),
    searchParams.sort.direction,
  ).then(({ elements, query, attributes, pagination: { count, total, page, per_page: perPage } }) => ({
    list: elements,
    attributes: attributes,
    pagination: {
      count,
      total,
      page,
      perPage,
      query,
    },
  }));

const useInputStreamRules = (
  inputId: string,
  searchParams: SearchParams,
  { enabled } = { enabled: true },
): {
  data: PaginatedInputStreamRulesResponse;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: keyFn(inputId, searchParams),
    queryFn: () =>
      defaultOnError(
        fetchInputStreamRules(inputId, searchParams),
        'Loading input stream rules failed with status',
        'Could not load input stream rules',
      ),
    placeholderData: keepPreviousData,
    enabled,
  });

  return {
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  };
};

export default useInputStreamRules;
