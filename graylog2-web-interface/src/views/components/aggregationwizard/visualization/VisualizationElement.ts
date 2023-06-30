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
import isEmpty from 'lodash/isEmpty';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { VisualizationType } from 'views/types';
import type { AggregationWidgetConfigBuilder } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';

import VisualizationConfiguration from './VisualizationConfiguration';

import type { AggregationElement } from '../AggregationElementType';
import type { VisualizationConfigFormValues, WidgetConfigFormValues } from '../WidgetConfigForm';

const findVisualizationType = (visualizationType: string) => {
  const visualizationTypeDefinition = PluginStore.exports('visualizationTypes').find(({ type }) => (type === visualizationType));

  if (!visualizationTypeDefinition) {
    throw new Error(`Invalid visualization type: ${visualizationType}`);
  }

  return visualizationTypeDefinition;
};

const defaultToConfig = () => undefined;

const formValuesToVisualizationConfig = (visualizationType: string, formValues: VisualizationConfigFormValues) => {
  const { config: { toConfig = defaultToConfig } = {} } = findVisualizationType(visualizationType);

  return toConfig(formValues);
};

const defaultFromConfig = () => ({});

const visualizationConfigToFormValues = (visualizationType: string, config: VisualizationConfig | undefined) => {
  const { config: { fromConfig = defaultFromConfig } = {} } = findVisualizationType(visualizationType);

  return fromConfig(config);
};

const fromConfig = (config: AggregationWidgetConfig) => ({
  visualization: {
    type: config.visualization,
    config: visualizationConfigToFormValues(config.visualization, config.visualizationConfig),
    eventAnnotation: config.eventAnnotation ?? false,
  },
});

const toConfig = (formValues: WidgetConfigFormValues, configBuilder: AggregationWidgetConfigBuilder) => configBuilder
  .visualization(formValues.visualization.type)
  .visualizationConfig(formValuesToVisualizationConfig(formValues.visualization.type, formValues.visualization.config))
  .eventAnnotation(formValues.visualization.eventAnnotation);

const hasErrors = (errors: {}) => Object.values(errors)
  .filter((value) => value !== undefined)
  .length > 0;

const validateConfig = (visualizationType: VisualizationType<any>, config: VisualizationConfigFormValues) => {
  const { fields = [] } = visualizationType.config;

  return fields
    .filter((field) => 'required' in field && field.required)
    .filter((field) => !field.isShown || field.isShown(config))
    .filter(({ name }) => config[name] === undefined || config[name] === '')
    .map(({ name, title }) => ({ [name]: `${title} is required.` }))
    .reduce((prev, cur) => ({ ...prev, ...cur }), {});
};

const validate = (formValues: WidgetConfigFormValues) => {
  const { visualization: { type, config } } = formValues;

  if (!type) {
    return { 'visualization.type': 'Type is required.' };
  }

  const visualizationType = findVisualizationType(type);

  const visualizationErrors = visualizationType.validate?.(formValues) ?? {};

  const configErrors = visualizationType.config
    ? validateConfig(visualizationType, config)
    : {};

  return hasErrors(configErrors) || hasErrors(visualizationErrors)
    ? { visualization: { ...visualizationErrors, config: configErrors } }
    : {};
};

const VisualizationElement: AggregationElement<'visualization'> = {
  title: 'Visualization',
  key: 'visualization',
  order: 4,
  allowCreate: (formValues: WidgetConfigFormValues) => isEmpty(formValues.visualization),
  component: VisualizationConfiguration,
  fromConfig,
  toConfig,
  validate,
  isEmpty: () => false,
};

export default VisualizationElement;
