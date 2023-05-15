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

export type RuleBuilderRule = {
  rule_builder: RuleBuilderType,
  description: string,
  created_at: string,
  id: string,
  title: string,
  modified_at: string,
}

type RuleBlockField = {
  [key:string]: string | number | boolean
}

export type RuleBlock = {
  function: string,
  parameters: RuleBlockField,
  output?: string,
  errors?: Array<string>
}

export type RuleBuilderType = {
  errors?: Array<string>,
  conditions: Array<RuleBlock>,
  actions: Array<RuleBlock>
}

export enum RuleBuilderSupportedTypes {
  String = 'java.lang.String',
  Number = 'java.lang.Number',
  Boolean = 'java.lang.Boolean',
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
    conditionsDict: [
      {
        name: 'has_field',
        pure: false,
        return_type: 'java.lang.Boolean',
        params: [
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'field',
            optional: false,
            primary: false,
            description: 'The field to check',
          },
          {
            type: 'org.graylog2.plugin.Message',
            transformed_type: 'org.graylog2.plugin.Message',
            name: 'message',
            optional: true,
            primary: false,
            description: "The message to use, defaults to '$message'",
          },
        ],
        description: 'Checks whether a message contains a value for a field',
        rule_builder_enabled: true,
        rule_builder_title: 'Message has field "$field"',
      },
      {
        name: 'has_field_equals',
        pure: false,
        return_type: 'java.lang.Boolean',
        params: [
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'field',
            optional: false,
            primary: false,
            description: null,
          },
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'fieldValue',
            optional: false,
            primary: false,
            description: null,
          },
        ],
        description: null,
        rule_builder_enabled: true,
        rule_builder_title: null,
      },
    ],
    actionsDict: [
      {
        name: 'to_string',
        pure: false,
        return_type: 'java.lang.String',
        params: [
          {
            type: 'java.lang.Object',
            transformed_type: 'java.lang.Object',
            name: 'value',
            optional: false,
            primary: false,
            description: 'Value to convert',
          },
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'default',
            optional: true,
            primary: false,
            description: "Used when 'value' is null, defaults to \"\"",
          },
        ],
        description: 'Converts a value to its string representation',
        rule_builder_enabled: true,
        rule_builder_title: null,
      },
      {
        name: 'has_field',
        pure: false,
        return_type: 'java.lang.Boolean',
        params: [
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'field',
            optional: false,
            primary: false,
            description: 'The field to check',
          },
          {
            type: 'org.graylog2.plugin.Message',
            transformed_type: 'org.graylog2.plugin.Message',
            name: 'message',
            optional: true,
            primary: false,
            description: "The message to use, defaults to '$message'",
          },
        ],
        description: 'Checks whether a message contains a value for a field',
        rule_builder_enabled: true,
        rule_builder_title: 'Message has field "$field"',
      },
      {
        name: 'get_field',
        pure: false,
        return_type: 'java.lang.Object',
        params: [
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'field',
            optional: false,
            primary: false,
            description: 'The field to get',
          },
          {
            type: 'org.graylog2.plugin.Message',
            transformed_type: 'org.graylog2.plugin.Message',
            name: 'message',
            optional: true,
            primary: false,
            description: "The message to use, defaults to '$message'",
          },
        ],
        description: 'Retrieves the value for a field',
        rule_builder_enabled: true,
        rule_builder_title: null,
      },
      {
        name: 'set_field',
        pure: false,
        return_type: 'java.lang.Void',
        params: [
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'field',
            optional: false,
            primary: false,
            description: 'The new field name',
          },
          {
            type: 'java.lang.Object',
            transformed_type: 'java.lang.Object',
            name: 'value',
            optional: false,
            primary: false,
            description: 'The new field value',
          },
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'prefix',
            optional: true,
            primary: false,
            description: 'The prefix for the field name',
          },
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'suffix',
            optional: true,
            primary: false,
            description: 'The suffix for the field name',
          },
          {
            type: 'org.graylog2.plugin.Message',
            transformed_type: 'org.graylog2.plugin.Message',
            name: 'message',
            optional: true,
            primary: false,
            description: "The message to use, defaults to '$message'",
          },
          {
            type: 'java.lang.Object',
            transformed_type: 'java.lang.Object',
            name: 'default',
            optional: true,
            primary: false,
            description: 'Used when value not available',
          },
        ],
        description: 'Sets a new field in a message',
        rule_builder_enabled: true,
        rule_builder_title: null,
      },
      {
        name: 'substring',
        pure: false,
        return_type: 'java.lang.String',
        params: [
          {
            type: 'java.lang.String',
            transformed_type: 'java.lang.String',
            name: 'value',
            optional: false,
            primary: false,
            description: 'The string to extract from',
          },
          {
            type: 'java.lang.Long',
            transformed_type: 'java.lang.Long',
            name: 'start',
            optional: false,
            primary: false,
            description: 'The position to start from, negative means count back from the end of the String by this many characters',
          },
          {
            type: 'java.lang.Long',
            transformed_type: 'java.lang.Long',
            name: 'end',
            optional: true,
            primary: false,
            description: 'The position to end at (exclusive), negative means count back from the end of the String by this many characters, defaults to length of the input string',
          },
        ],
        description: 'Extract a substring from a string',
        rule_builder_enabled: true,
        rule_builder_title: null,
      },
    ],
    rule: {
      title: 'Rulebuilder rule {{$timestamp}}',
      description: 'generated by the mighty Rulebuilder',
      rule_builder: {
        conditions: [
          {
            function: 'has_field',
            parameters: {
              field: 'message',
            },
          },
        ],
        actions: [
          {
            function: 'get_field',
            parameters: {
              field: 'message',
            },
            outputvariable: 'field1',
          },
          {
            function: 'set_field',
            parameters: {
              field: 'set_this_field',
              value: '$field1',
            },
          },
        ],
      },
    },
    refetchRule,
    refetchConditionsDict,
    refetchActionsDict,
    createRule,
    updateRule,
    fetchValidateRule,
  };
};

export default useRuleBuilder;
