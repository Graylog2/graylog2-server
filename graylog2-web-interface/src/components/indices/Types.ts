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

export type MaintenanceOptions = {
  strategies: Array<unknown>
}

export type IndicesConfigurationActionsType = {
  loadRotationStrategies: () => Promise<MaintenanceOptions>,
  loadRetentionStrategies: () => Promise<MaintenanceOptions>,
};

export interface RotationStrategyContext {
  time_size_optimizing_retention_fixed_leeway?: string,
}

export type IndicesConfigurationStoreState = {
  activeRotationConfig: any,
  rotationStrategies: any,
  activeRetentionConfig: any,
  retentionStrategies: any,
  retentionStrategiesContext: RetentionStrategyContext,
  rotationStrategiesContext: RotationStrategyContext,
}
export type SizeBasedRotationStrategyConfig = {
  type: string,
  max_size: number,
}
export type MessageCountRotationStrategyConfig = {
  type: string,
  max_docs_per_index: number,
}
export type TimeBasedRotationStrategyConfig = {
  type: string,
  rotation_period?: string,
  max_rotation_period?: string,
  rotate_empty_index_set?: boolean,
}
export type RotationStrategyConfig =
  SizeBasedRotationStrategyConfig
  | MessageCountRotationStrategyConfig
  | TimeBasedRotationStrategyConfig;
export type RetentionStrategyConfig = {
  type?: string,
  max_number_of_indices?: number,
  index_action?: string,
}

export interface JsonSchemaStringPropertyType {
  type: string,
}

export interface JsonSchemaIndexActionPropertyType {
  type: string,

  enum: Array<string>,
}

export interface JsonSchemaBooleanPropertyType {
  type: string;
}

export interface RotationProperties {
  rotation_period?: JsonSchemaStringPropertyType,

  max_rotation_period?: JsonSchemaStringPropertyType,

  type: JsonSchemaStringPropertyType,

  max_size?: JsonSchemaStringPropertyType,

  rotate_empty_index_set?: JsonSchemaBooleanPropertyType,

  index_lifetime_max?: JsonSchemaBooleanPropertyType,

  index_lifetime_min?: JsonSchemaBooleanPropertyType,
}

export interface RotationJsonSchema {
  type: string,

  id: string,

  properties: RotationProperties,
}

export interface RetentionProperties {
  max_number_of_indices: JsonSchemaStringPropertyType,

  type: JsonSchemaStringPropertyType,

  index_action?: JsonSchemaIndexActionPropertyType,
}

export interface RetentionJsonSchema {
  type: string,

  id: string,

  properties: RetentionProperties,
}

export interface RotationStrategy {
  type: string,

  default_config: RotationStrategyConfig,

  json_schema: RotationJsonSchema,
}

export interface RetentionStrategy {
  type?: string,

  default_config?: RetentionStrategyConfig,

  json_schema?: RetentionJsonSchema,
}

export interface RetentionStrategyContext {
  max_index_retention_period?: string,
}

export interface RotationStrategyResponse {
  total: number,

  context: RotationStrategyContext,

  strategies: Array<RotationStrategy>,
}

export interface RetentionStrategyResponse {
  total: number,

  strategies: Array<RetentionStrategy>,

  context: RetentionStrategyContext,
}

export type JsonSchema = RotationJsonSchema | RetentionJsonSchema

export type Strategy = RotationStrategy | RetentionStrategy

export type Strategies = Array<Strategy>

export type StrategyConfig = RotationStrategyConfig | RetentionStrategyConfig
