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
import { useContext } from 'react';
import { useQuery } from '@tanstack/react-query';

import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { PipelineRulesContext } from 'components/rules/RuleContext';

export type RuleBuilderRule = {
  rule_builder: RuleBuilderType,
  description: string,
  created_at?: string,
  id?: string,
  title: string,
  modified_at?: string,
}

type RuleBlockField = {
  [key:string]: string | number | boolean
}

export type RuleBlock = {
  function: string,
  params: RuleBlockField,
  output?: string,
  errors?: Array<string>
}

export type RuleBuilderType = {
  errors?: Array<string>,
  conditions: Array<RuleBlock>,
  actions: Array<RuleBlock>
}

export enum RuleBuilderSupportedTypes {
  Boolean = 'java.lang.Boolean',
  Message = 'org.graylog2.plugin.Message',
  Number = 'java.lang.Long',
  Object = 'java.lang.Object',
  String = 'java.lang.String',
}

export type BlockFieldDict = {
  type: RuleBuilderSupportedTypes,
  transformed_type: RuleBuilderSupportedTypes,
  name: string,
  optional: boolean,
  primary: boolean,
  description: string | null
}

export type BlockDict = {
  name: string,
  pure: boolean,
  return_type: RuleBuilderSupportedTypes,
  params: Array<BlockFieldDict>,
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
      'PUT',
      qualifyUrl(ApiRoutes.RuleBuilderController.update(rule.id).url),
      rule,
    );
  } catch (errorThrown) {
    UserNotification.error(`Updating the Rule Builder Rule failed with status: ${errorThrown}`, 'Could not Update the Rule Builder Rule.');
  }
};

const deleteRule = async (ruleId: string) => {
  try {
    await fetch(
      'DELETE',
      qualifyUrl(ApiRoutes.RulesController.delete(ruleId).url),
    );
  } catch (errorThrown) {
    UserNotification.error(`Deleting the Rule Builder Rule failed with status: ${errorThrown}`, 'Could not Delete the Rule Builder Rule.');
  }
};

const fetchValidateRule = async (rule: RuleBuilderRule): Promise<RuleBuilderRule> => fetch(
  'POST',
  qualifyUrl(ApiRoutes.RuleBuilderController.validate().url),
  rule,
);

const fetchConditionsDict = async () => fetch('GET', qualifyUrl(ApiRoutes.RuleBuilderController.listConditionsDict().url));
const fetchActionsDict = async () => fetch('GET', qualifyUrl(ApiRoutes.RuleBuilderController.listActionsDict().url));

const useRuleBuilder = () => {
  const { rule } = useContext(PipelineRulesContext);

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
    isLoadingConditionsDict,
    isLoadingActionsDict,
    conditionsDict,
    actionsDict,
    rule: rule as RuleBuilderRule|null,
    refetchConditionsDict,
    refetchActionsDict,
    createRule,
    updateRule,
    deleteRule,
    fetchValidateRule,
  };
};

export default useRuleBuilder;
