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

import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import { defaultOnError } from 'util/conditional/onError';
import i18n from 'i18n';
import type { InputPipelineRule } from 'components/inputs/InputDiagnosis/types';

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

type SortType = 'rule' | 'pipeline' | 'connected_streams';

export const INPUT_PIPELINE_RULES_QUERY_KEY = 'input_pipeline_rules';

export const keyFn = (inputId: string, searchParams: SearchParams) => [
  INPUT_PIPELINE_RULES_QUERY_KEY,
  inputId,
  searchParams,
];

export type PaginatedInputPipelineRulesResponse = {
  list: Readonly<Array<InputPipelineRule>>;
  pagination: { total: number };
  attributes: Array<Attribute>;
};

export const fetchInputPipelineRules = async (
  inputId: string,
  searchParams: SearchParams,
): Promise<PaginatedResponse<InputPipelineRule>> =>
  SystemInputs.getPipelineRulesPage(
    inputId,
    searchParams.sort.attributeId as SortType,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
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

const useInputPipelineRules = (
  inputId: string,
  searchParams: SearchParams,
  { enabled } = { enabled: true },
): {
  data: PaginatedInputPipelineRulesResponse;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: keyFn(inputId, searchParams),
    queryFn: () =>
      defaultOnError(
        fetchInputPipelineRules(inputId, searchParams),
        i18n.t('diagnosis.rules.load_pipeline_rules_failed', { ns: 'inputs' }),
        i18n.t('diagnosis.rules.could_not_load_pipeline_rules', { ns: 'inputs' }),
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

export default useInputPipelineRules;
