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

import { StreamDestinationsFiltersBuilder } from '@graylog/server-api';

import type { BlockDict, RuleBuilderRule } from 'components/rules/rule-builder/types';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import { defaultOnError } from 'util/conditional/onError';

type ConditionsResponse ={conditions: Array<BlockDict>}
const fetchRuleConditions = async () => StreamDestinationsFiltersBuilder.getConditions().then((resp: ConditionsResponse) => resp.conditions);

export const fetchValidateRule = async (currentRule: StreamOutputFilterRule): Promise<StreamOutputFilterRule> => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { stream_id, destination_type, status, source, rule: rule_builder, ...ruleToValidate }: any = currentRule;

  return StreamDestinationsFiltersBuilder.validateRule({ rule_builder, ...ruleToValidate }).then((resp: { rule_builder: RuleBuilderRule }) => {
    const { rule_builder: { rule_builder: rule, ...validRule } } = resp;

    return { ...currentRule, rule, ...validRule };
  });
};

const useStreamOutputRuleBuilder = () => {
  const { data: conditions, refetch: refetchConditions, isFetching: isLoadingConditions } = useQuery<Array<BlockDict>>(
    ['stream', 'filter', 'conditions'],
    () => defaultOnError(fetchRuleConditions(), 'Loading Stream Output Filter Rule Builder Conditions list failed with status', 'Could not load Stream Output Filter Rule Builder Conditions list.'),
    {
      keepPreviousData: true,
    },
  );

  return {
    conditions: conditions,
    refetchConditions,
    isLoadingConditions,
  };
};

export default useStreamOutputRuleBuilder;
