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

export type BlockType = 'condition' | 'action'

export interface ObjectWithErrors {
  errors?: Array<string>,
}

export type RuleBuilderRule = {
  rule_builder: RuleBuilderType,
  description?: string,
  created_at?: string,
  id?: string,
  source?: string,
  title: string,
  modified_at?: string,
  simulator_message?: string,
}

export type RuleBlockField = {
  [key:string]: string | number | boolean
}

export interface RuleBlock extends ObjectWithErrors {
  function: string,
  id: string,
  params: RuleBlockField,
  outputvariable?: string,
  negate?: boolean,
  step_title?: string,
  errors?: Array<string>
}

export interface RuleBuilderType extends ObjectWithErrors {
  errors?: Array<string>,
  operator?: 'AND'|'OR',
  conditions: Array<RuleBlock>,
  actions: Array<RuleBlock>
}

export enum RuleBuilderTypes {
  Boolean = 'java.lang.Boolean',
  Message = 'org.graylog2.plugin.Message',
  Number = 'java.lang.Long',
  Object = 'java.lang.Object',
  String = 'java.lang.String',
  Void = 'java.lang.Void',
  DateTime = 'org.joda.time.DateTime',
  DateTimeZone = 'org.joda.time.DateTimeZone',
  DateTimeFormatter = 'org.joda.time.format.DateTimeFormatter'
}

export const RULE_BUILDER_TYPES_WITH_OUTPUT = [
  RuleBuilderTypes.Boolean,
  RuleBuilderTypes.Number,
  RuleBuilderTypes.String,
  RuleBuilderTypes.Object,
  RuleBuilderTypes.Message,
  RuleBuilderTypes.DateTime,
] as const;

export type BlockFieldDict = {
  type: RuleBuilderTypes,
  transformed_type: RuleBuilderTypes,
  name: string,
  optional: boolean,
  rule_builder_variable: boolean,
  allow_negatives: boolean,
  description: string | null,
  default_value?: unknown,
}

export type BlockDict = {
  name: string,
  pure: boolean,
  return_type: RuleBuilderTypes,
  params: Array<BlockFieldDict>,
  description: string | null,
  rule_builder_enabled: boolean,
  rule_builder_function_group: string,
  rule_builder_name: string,
  rule_builder_title: string | null
}

export type OutputVariables = Array<{
  variableName: string,
  variableType?: RuleBuilderTypes,
  stepOrder: number,
  blockId: string
}>
