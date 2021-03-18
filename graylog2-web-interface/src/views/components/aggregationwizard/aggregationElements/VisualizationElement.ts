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
import { isEmpty } from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';

import type { AggregationElement } from './AggregationElementType';

import VisualizationConfiguration from '../elementConfiguration/VisualizationConfiguration';
import { VisualizationConfigFormValues, WidgetConfigFormValues } from '../WidgetConfigForm';

const findVisualizationType = (visualizationType: string) => {
  const visualizationTypeDefinition = PluginStore.exports('visualizationTypes').find(({ type }) => (type === visualizationType));

  if (!visualizationTypeDefinition) {
    throw new Error(`Invalid visualization type: ${visualizationType}`);
  }

  return visualizationTypeDefinition;
};

const formValuesToVisualizationConfig = (visualizationType: string, formValues: VisualizationConfigFormValues) => {
  const { config: { toConfig } } = findVisualizationType(visualizationType);

  return toConfig(formValues);
};

const visualizationConfigToFormValues = (visualizationType: string, config: VisualizationConfig) => {
  const { config: { fromConfig } } = findVisualizationType(visualizationType);

  return fromConfig(config);
};

const fromConfig = (config: AggregationWidgetConfig) => ({
  visualization: {
    type: config.visualization,
    config: visualizationConfigToFormValues(config.visualization, config.visualizationConfig),
  },
});
const toConfig = (formValues: WidgetConfigFormValues, currentConfig: AggregationWidgetConfig) => currentConfig
  .toBuilder()
  .visualization(formValues.visualization.type)
  .visualizationConfig(formValuesToVisualizationConfig(formValues.visualization.type, formValues.visualization.config))
  .build();

const hasErrors = (errors: { [key: string]: string }) => Object.values(errors)
  .filter((value) => value !== undefined)
  .length > 0;

const validate = (formValues: WidgetConfigFormValues) => {
  const { visualization: { type, config } } = formValues;

  if (!type) {
    return { 'visualization.type': 'Type is required.' };
  }

  const visualizationType = findVisualizationType(type);

  if (visualizationType.config) {
    const { fields } = visualizationType.config;

    const configErrors = fields
      .filter((field) => 'required' in field && field.required)
      .filter((field) => !field.isShown || field.isShown(config))
      .filter(({ name }) => config[name] === undefined || config[name] === '')
      .map(({ name, title }) => ({ [name]: `${title} is required.` }))
      .reduce((prev, cur) => ({ ...prev, ...cur }), {});

    return hasErrors(configErrors) ? { visualization: { config: configErrors } } : {};
  }

  return {};
};

const VisualizationElement: AggregationElement = {
  title: 'Visualization',
  key: 'visualization',
  order: 4,
  allowCreate: (formValues: WidgetConfigFormValues) => isEmpty(formValues.visualization),
  component: VisualizationConfiguration,
  fromConfig,
  toConfig,
  validate,
};

export default VisualizationElement;
