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
import PropTypes from 'prop-types';

export type BlockType = 'condition' | 'action'

export type RuleBuilderRule = {
  rule_builder: RuleBuilderType,
  description?: string,
  created_at?: string,
  id?: string,
  title: string,
  modified_at?: string,
}

export type RuleBlockField = {
  [key:string]: string | number | boolean
}

export type RuleBlock = {
  function: string,
  params: RuleBlockField,
  outputvariable?: string,
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

export const ruleBlockPropType = PropTypes.shape({
  function: PropTypes.string.isRequired,
  params: PropTypes.objectOf(PropTypes.oneOfType([PropTypes.string, PropTypes.number, PropTypes.bool])).isRequired,
  output: PropTypes.string,
  errors: PropTypes.arrayOf(PropTypes.string),
});

export const blockDictPropType = PropTypes.shape({
  name: PropTypes.string.isRequired,
  pure: PropTypes.bool.isRequired,
  return_type: PropTypes.string.isRequired,
  params: PropTypes.arrayOf(PropTypes.shape({
    type: PropTypes.string.isRequired,
    transformed_type: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    optional: PropTypes.bool.isRequired,
    primary: PropTypes.bool.isRequired,
    description: PropTypes.string,
  })).isRequired,
  description: PropTypes.string,
  rule_builder_enabled: PropTypes.bool.isRequired,
  rule_builder_title: PropTypes.string,
});
