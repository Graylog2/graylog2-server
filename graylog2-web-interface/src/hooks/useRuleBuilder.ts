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
import type { RuleBuilderType } from 'components/rules/RuleBuilder';

export type RuleBuilderRule = {
  rule_builder: RuleBuilderType,
  description: string,
  created_at: string,
  id: string,
  title: string,
  modified_at: string,
}

export type BlockDictionary = {
        name: string,
        pure: boolean,
        return_type: string,
        params: Array<
          {
            type: string,
            transformed_type: string,
            name: string,
            optional: boolean,
            primary: boolean,
            description: string | null
          }
        >,
        description: string | null,
        rule_builder_enabled: boolean,
        rule_builder_title: string | null
    }

const createRule = async (rule: RuleBuilderRule) => {
  try {
    await fetch(
      'POST',
      qualifyUrl(ApiRoutes.RuleBuilderController.create().url),
      rule,
    );
  } catch (errorThrown) {
    UserNotification.error(`Creating the Rule Builder Rule failed with status: ${errorThrown}`, 'Could not Create the Rule Builder Rule.');
  }
};

const updateRule = async (rule: RuleBuilderRule) => {
  try {
    await fetch(
      'POST',
      qualifyUrl(ApiRoutes.RuleBuilderController.update(rule.id).url),
      rule,
    );
  } catch (errorThrown) {
    UserNotification.error(`Updating the Rule Builder Rule failed with status: ${errorThrown}`, 'Could not Update the Rule Builder Rule.');
  }
};

const fetchValidateRule = async (rule: RuleBuilderRule): Promise<RuleBuilderRule> => fetch(
  'POST',
  qualifyUrl(ApiRoutes.RuleBuilderController.validate().url),
  rule,
);

const fetchRule = async (ruleId: string): Promise<RuleBuilderRule> => fetch('GET', qualifyUrl(ApiRoutes.RuleBuilderController.get(ruleId).url));
const fetchConditionsDict = async () => fetch('GET', qualifyUrl(ApiRoutes.RuleBuilderController.listConditionsDict().url));
const fetchActionsDict = async () => fetch('GET', qualifyUrl(ApiRoutes.RuleBuilderController.listActionsDict().url));

const useRuleBuilder = (rule?: RuleBuilderRule) => {
  const { data, refetch: refetchRule, isFetching: isLoadingRule } = useQuery<RuleBuilderRule>(
    ['rule'],
    () => fetchRule(rule?.id),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Rule Builder Rule list failed with status: ${errorThrown}`,
          'Could not load Rule Builder Rule list.');
      },
      keepPreviousData: true,
    },
  );
  const { data: conditionsDict, refetch: refetchConditionsDict, isFetching: isLoadingConditionsDict } = useQuery<Array<BlockDictionary>>(
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
  const { data: actionsDict, refetch: refetchActionsDict, isFetching: isLoadingActionsDict } = useQuery<Array<BlockDictionary>>(
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
    rule: data,
    refetchRule,
    refetchConditionsDict,
    refetchActionsDict,
    createRule,
    updateRule,
    fetchValidateRule,
  };
};

export default useRuleBuilder;
