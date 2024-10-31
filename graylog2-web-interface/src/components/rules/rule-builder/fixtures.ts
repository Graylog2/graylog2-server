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
import type { BlockDict, RuleBlock } from './types';
import { RuleBuilderTypes } from './types';

const conditionsBlockDict: BlockDict[] = [
  {
    name: 'has_field_less_or_equal',
    pure: false,
    return_type: RuleBuilderTypes.Boolean,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Message field to check against',
      },
      {
        type: RuleBuilderTypes.Number,
        transformed_type: RuleBuilderTypes.Number,
        name: 'fieldValue',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Field value to check for',
      },
    ],
    description: "Checks if the message has a field and if this field's numeric value is less than or equal to the given fieldValue",
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Field 'field' less than or equal 'fieldValue'",
    rule_builder_name: 'has field less or equal',
  },
  {
    name: 'has_field',
    pure: false,
    return_type: RuleBuilderTypes.Boolean,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The field to check',
      },
      {
        type: RuleBuilderTypes.Message,
        transformed_type: RuleBuilderTypes.Message,
        name: 'message',
        optional: true,
        rule_builder_variable: true,
        allow_negatives: false,
        description: "The message to use, defaults to '$message'",
      },
    ],
    description: 'Checks whether a message contains a value for a field',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Message has field 'field'",
    rule_builder_name: 'has field',
  },
  {
    name: 'has_field_greater_or_equal',
    pure: false,
    return_type: RuleBuilderTypes.Boolean,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Message field to check against',
      },
      {
        type: RuleBuilderTypes.Number,
        transformed_type: RuleBuilderTypes.Number,
        name: 'fieldValue',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Field value to check for',
      },
    ],
    description: "Checks if the message has a field and if this field's numeric value is greater than or equal to the given fieldValue",
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Field 'field' greater than or equal 'fieldValue'",
    rule_builder_name: 'has field greater than or equal',

  },
  {
    name: 'has_field_equals',
    pure: false,
    return_type: RuleBuilderTypes.Boolean,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Message field to check against',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'fieldValue',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Field value to check for',
      },
    ],
    description: "Checks if the message has a field and if this field's string value is equal to the given fieldValue",
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Field 'field' equals 'fieldValue'",
    rule_builder_name: 'field equals',
  },
];

const actionsBlockDict: BlockDict[] = [
  {
    name: 'has_field',
    pure: false,
    return_type: RuleBuilderTypes.Boolean,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The field to check',
      },
      {
        type: RuleBuilderTypes.Message,
        transformed_type: RuleBuilderTypes.Message,
        name: 'message',
        optional: true,
        rule_builder_variable: true,
        allow_negatives: false,
        description: "The message to use, defaults to '$message'",
      },
    ],
    description: 'Checks whether a message contains a value for a field',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Message has field 'field'",
    rule_builder_name: 'has field',
  },
  {
    name: 'to_long',
    pure: false,
    return_type: RuleBuilderTypes.Number,
    params: [
      {
        type: RuleBuilderTypes.Object,
        transformed_type: RuleBuilderTypes.Object,
        name: 'value',
        optional: false,
        rule_builder_variable: true,
        allow_negatives: false,
        description: 'Value to convert',
      },
      {
        type: RuleBuilderTypes.Number,
        transformed_type: RuleBuilderTypes.Number,
        name: 'default',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: true,
        description: "Used when 'value' is null, defaults to 0",
      },
    ],
    description: 'Converts a value to a long value using its string representation',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: 'Convert value to number',
    rule_builder_name: 'to long',
  },
  {
    name: 'get_field',
    pure: false,
    return_type: RuleBuilderTypes.Object,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The field to get',
      },
      {
        type: RuleBuilderTypes.Message,
        transformed_type: RuleBuilderTypes.Message,
        name: 'message',
        optional: true,
        rule_builder_variable: true,
        allow_negatives: false,
        description: "The message to use, defaults to '$message'",
      },
    ],
    description: 'Retrieves the value for a field',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Retrieve value for field 'field'",
    rule_builder_name: 'get field',
  },
  {
    name: 'set_grok_to_fields',
    pure: false,
    return_type: RuleBuilderTypes.Void,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Field to extract and apply grok pattern to',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'grokPattern',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Grok pattern to apply',
      },
      {
        type: RuleBuilderTypes.Boolean,
        transformed_type: RuleBuilderTypes.Boolean,
        name: 'grokNamedOnly',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Set to true to only set fields for named captures',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'prefix',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Prefix for created fields',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'suffix',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Suffix for created fields',
      },
    ],
    description: 'Match grok pattern and set fields',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Match grok pattern on field 'field' and set fields",
    rule_builder_name: 'match grok',
  },
  {
    name: 'substring',
    pure: false,
    return_type: RuleBuilderTypes.String,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'value',
        optional: false,
        rule_builder_variable: true,
        allow_negatives: false,
        description: 'The string to extract from',
      },
      {
        type: RuleBuilderTypes.Number,
        transformed_type: RuleBuilderTypes.Number,
        name: 'start',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: true,
        description: 'The position to start from, negative means count back from the end of the String by this many characters',
      },
      {
        type: RuleBuilderTypes.Number,
        transformed_type: RuleBuilderTypes.Number,
        name: 'indexEnd',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: true,
        description: 'The position to end at (exclusive), negative means count back from the end of the String by this many characters, defaults to length of the input string',
      },
    ],
    description: 'Extract a substring from a string',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Get substring from 'start' to 'end!\"end\"' of value",
    rule_builder_name: 'get substring',
  },
  {
    name: 'to_string',
    pure: false,
    return_type: RuleBuilderTypes.String,
    params: [
      {
        type: RuleBuilderTypes.Object,
        transformed_type: RuleBuilderTypes.Object,
        name: 'value',
        optional: false,
        rule_builder_variable: true,
        allow_negatives: false,
        description: 'Value to convert',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'default',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: "Used when 'value' is null, defaults to \"\"",
      },
    ],
    description: 'Converts a value to its string representation',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: 'Convert value to string',
    rule_builder_name: 'convert to string',
  },
  {
    name: 'set_field',
    pure: false,
    return_type: RuleBuilderTypes.Void,
    params: [
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'field',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The new field name',
      },
      {
        type: RuleBuilderTypes.Object,
        transformed_type: RuleBuilderTypes.Object,
        name: 'value',
        optional: false,
        rule_builder_variable: true,
        allow_negatives: false,
        description: 'The new field value',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'prefix',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The prefix for the field name',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.String,
        name: 'suffix',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The suffix for the field name',
      },
      {
        type: RuleBuilderTypes.Message,
        transformed_type: RuleBuilderTypes.Message,
        name: 'message',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: "The message to use, defaults to '$message'",
      },
      {
        type: RuleBuilderTypes.Object,
        transformed_type: RuleBuilderTypes.Object,
        name: 'default',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'Used when value not available',
      },
    ],
    description: 'Sets a new field in a message',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Set value to field 'field'",
    rule_builder_name: 'set value',
  },
  {
    name: 'format_date',
    pure: false,
    return_type: RuleBuilderTypes.String,
    params: [
      {
        type: RuleBuilderTypes.DateTime,
        transformed_type: RuleBuilderTypes.DateTime,
        name: 'value',
        optional: false,
        rule_builder_variable: true,
        allow_negatives: false,
        description: 'The date to format',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.DateTimeFormatter,
        name: 'format',
        optional: false,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The format string to use, see http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html',
      },
      {
        type: RuleBuilderTypes.String,
        transformed_type: RuleBuilderTypes.DateTimeZone,
        name: 'timezone',
        optional: true,
        rule_builder_variable: false,
        allow_negatives: false,
        description: 'The timezone to apply to the date, defaults to UTC',
      },
    ],
    description: 'Formats a date using the given format string',
    rule_builder_enabled: true,
    rule_builder_function_group: 'g1',
    rule_builder_title: "Format date (format 'format')",
    rule_builder_name: 'format date',
  },
];

const buildRuleBlock = (attrs: {
  functionName?: string,
  id?: string,
  params?: {[key:string]: string | number | boolean},
  outputvariable?: string,
  negate?: boolean,
  step_title?: string,
  errors?: Array<string>
} = {}) : RuleBlock => {
  const defaults = {
    functionName: 'to_long',
    id: 'random_id',
    params: {},
    step_title: 'to_long "foo"',
  };

  const { functionName, id, params, step_title } = { ...defaults, ...attrs };
  const optionalProperties = ['outputvariable', 'negate', 'errors'];

  const block: RuleBlock = {
    function: functionName, id, params, step_title,
  };

  optionalProperties.forEach((prop) => {
    if (attrs[prop]) {
      block[prop] = attrs[prop];
    }
  });

  return block;
};

export { actionsBlockDict, conditionsBlockDict, buildRuleBlock };
