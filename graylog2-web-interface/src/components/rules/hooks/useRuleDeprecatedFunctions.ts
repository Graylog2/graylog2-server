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

import { PipelinesPipelines } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = [];

const RULE_DEPRECATED_FUNCTIONS = 'rule_deprecated_functions';

export const getRuleDeprecatedFunctionsQueryKey = (ruleId: string) => [RULE_DEPRECATED_FUNCTIONS, ruleId];

const getDeprecatedFunctions = async ({ ruleId }: { ruleId: string }) =>
  PipelinesPipelines.getDeprecatedFunctionsForRule(ruleId);

const useRuleDeprecatedFunctions = (
  ruleId: string,
  { enabled } = { enabled: true },
): {
  data: Array<string>;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: getRuleDeprecatedFunctionsQueryKey(ruleId),
    queryFn: () =>
      defaultOnError(
        getDeprecatedFunctions({ ruleId }),
        'Loading deprecated functions for rule failed with status',
        'Could not load deprecated functions for rule metadata',
      ),
    enabled,
  });

  return {
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  };
};

export default useRuleDeprecatedFunctions;
