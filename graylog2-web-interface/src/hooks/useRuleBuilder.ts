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

import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import type { BlockDict, RuleBuilderRule } from 'components/rules/rule-builder/types';
import useParams from 'routing/useParams';

const createRule = async (rule: RuleBuilderRule) => {
  try {
    await fetch(
      'POST',
      qualifyUrl(ApiRoutes.RuleBuilderController.create().url),
      rule,
    );

    UserNotification.success(`Rule "${rule.title}" created successfully`);
  } catch (errorThrown) {
    console.log('createRule', errorThrown, rule);
    UserNotification.error(`Creating the Rule Builder Rule failed with status: ${errorThrown}`, 'Could not Create the Rule Builder Rule.');
  }
};

const updateRule = async (rule: RuleBuilderRule) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { source, errors, ...ruleToUpdate }: any = rule;

  try {
    await fetch(
      'PUT',
      qualifyUrl(ApiRoutes.RuleBuilderController.update(rule.id).url),
      ruleToUpdate,
    );

    UserNotification.success(`Rule "${rule.title}" updated successfully`);
  } catch (errorThrown) {
    console.log('updateRule', errorThrown, ruleToUpdate);
    UserNotification.error(`Updating the Rule Builder Rule failed with status: ${errorThrown}`, 'Could not Update the Rule Builder Rule.');
  }
};

const fetchValidateRule = async (rule: RuleBuilderRule): Promise<RuleBuilderRule> => fetch(
  'POST',
  qualifyUrl(ApiRoutes.RuleBuilderController.validate().url),
  rule,
);

const fetchRule = async (ruleId: string = '') => fetch('GET', qualifyUrl(ApiRoutes.RulesController.get(ruleId).url));
const fetchConditionsDict = async () => fetch('GET', qualifyUrl(ApiRoutes.RuleBuilderController.listConditionsDict().url));
const fetchActionsDict = async () => fetch('GET', qualifyUrl(ApiRoutes.RuleBuilderController.listActionsDict().url));

const useRuleBuilder = () => {
  const { ruleId } = useParams();

  const { data: rule, refetch: refetchRule, isFetching: isLoadingRule } = useQuery<RuleBuilderRule|null>(
    ['rule'],
    () => fetchRule(ruleId),
    {
      enabled: !(ruleId === 'new'),
      onError: (errorThrown) => {
        UserNotification.error(`Loading Rule Builder Rule failed with status: ${errorThrown}`,
          'Could not load Rule Builder Rule.');
      },
      keepPreviousData: true,
    },
  );
  const { data: conditionsDict, refetch: refetchConditionsDict, isFetching: isLoadingConditionsDict } = useQuery<Array<BlockDict>>(
    ['conditions'],
    fetchConditionsDict,
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Rule Builder Conditions list failed with status: ${errorThrown}`,
          'Could not load Rule Builder Conditions list.');
      },
      keepPreviousData: true,
    },
  );
  const { data: actionsDict, refetch: refetchActionsDict, isFetching: isLoadingActionsDict } = useQuery<Array<BlockDict>>(
    ['actions'],
    fetchActionsDict,
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Rule Builder Actions list failed with status: ${errorThrown}`,
          'Could not load Rule Builder Actions list.');
      },
      keepPreviousData: true,
    },
  );

  return {
    isLoadingRule,
    isLoadingConditionsDict,
    isLoadingActionsDict,
    conditionsDict,
    actionsDict,
    rule,
    refetchRule,
    refetchConditionsDict,
    refetchActionsDict,
    createRule,
    updateRule,
    fetchValidateRule,
  };
};

export default useRuleBuilder;
