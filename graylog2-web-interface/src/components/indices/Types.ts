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

export type IndicesConfigurationActionsType = {
  loadRotationStrategies: () => Promise<unknown>,
  loadRetentionStrategies: () => Promise<unknown>,
};
export type IndicesConfigurationStoreState = {
  activeRotationConfig: any,
  rotationStrategies: any,
  activeRetentionConfig: any,
  retentionStrategies: any,
  retentionStrategiesContext: RetentionStrategyContext,
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
  rotation_period: string,
  max_rotation_period: string,
}
export type RotationStrategyConfig = SizeBasedRotationStrategyConfig | MessageCountRotationStrategyConfig| TimeBasedRotationStrategyConfig;
export type RetentionStrategyConfig = {
  type: string,
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
export interface RotationProperties {
  rotation_period?: JsonSchemaStringPropertyType,
  max_rotation_period?: JsonSchemaStringPropertyType,
  type: JsonSchemaStringPropertyType,
  max_size?: JsonSchemaStringPropertyType,
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
  type: string,
  default_config: RetentionStrategyConfig,
  json_schema: RetentionJsonSchema,
}
export interface RetentionStrategyContext {
  max_index_retention_period?: string,
}
export interface RotationStrategyResponse {
  total: number,
  strategies: Array<RotationStrategy>,
}
export interface RetentionStrategyResponse {
  total: number,
  strategies: Array<RetentionStrategy>,
  context: RetentionStrategyContext,
}
export const RetentionStrategiesContextPropType = PropTypes.shape({
  max_index_retention_period: PropTypes.string,
});
export const SizeBasedRotationStrategyConfigPropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
  max_size: PropTypes.number.isRequired,
});
export const MessageCountRotationStrategyConfigPropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
  max_docs_per_index: PropTypes.number.isRequired,
});
export const TimeBasedRotationStrategyConfigPropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
  rotation_period: PropTypes.string.isRequired,
  max_rotation_period: PropTypes.string,
});

export const RotationStrategyConfigPropType = PropTypes.oneOfType([
  SizeBasedRotationStrategyConfigPropType,
  MessageCountRotationStrategyConfigPropType,
  TimeBasedRotationStrategyConfigPropType,
]);

export const IndexActionPropType = PropTypes.string;
export const RetentionStrategyConfigPropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
  max_number_of_indices: PropTypes.number,
  index_action: PropTypes.string,
});

export const JsonSchemaStringPropertyTypePropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
});
export const JsonSchemaIndexActionPropertyTypePropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
  enum: PropTypes.arrayOf(PropTypes.string).isRequired,
});
export const RotationPropertiesPropType = PropTypes.exact({
  rotation_period: JsonSchemaStringPropertyTypePropType,
  max_rotation_period: JsonSchemaStringPropertyTypePropType,
  type: JsonSchemaStringPropertyTypePropType.isRequired,
  max_size: JsonSchemaStringPropertyTypePropType,
  max_docs_per_index: JsonSchemaStringPropertyTypePropType,
});
export const RotationJsonSchemaPropType = PropTypes.exact({
  type: PropTypes.string,
  id: PropTypes.string,
  properties: RotationPropertiesPropType.isRequired,
});
export const RetentionPropertiesPropType = PropTypes.exact({
  max_number_of_indices: JsonSchemaStringPropertyTypePropType.isRequired,
  type: JsonSchemaStringPropertyTypePropType.isRequired,
  index_action: JsonSchemaIndexActionPropertyTypePropType,
});
export const RetentionJsonSchemaPropType = PropTypes.exact({
  type: PropTypes.string,
  id: PropTypes.string,
  properties: RetentionPropertiesPropType,
});
export const RotationStrategyPropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
  default_config: RotationStrategyConfigPropType.isRequired,
  json_schema: RotationJsonSchemaPropType.isRequired,
});
export const RetentionStrategyPropType = PropTypes.exact({
  type: PropTypes.string.isRequired,
  default_config: RetentionStrategyConfigPropType.isRequired,
  json_schema: RetentionJsonSchemaPropType.isRequired,
});
