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

import type { AggregationElement } from './AggregationElementType';

import VisualizationConfiguration from '../elementConfiguration/VisualizationConfiguration';
import { VisualizationConfigFormValues, WidgetConfigFormValues } from '../WidgetConfigForm';

const formValuesToVisualizationConfig = (visualizationType: string, formValues: VisualizationConfigFormValues) => {
  const visualizationTypeDefinition = PluginStore.exports('visualizationTypes').find(({ type }) => (type === visualizationType));

  if (!visualizationTypeDefinition) {
    throw new Error(`Invalid visualization type: ${visualizationType}`);
  }

  const { config: { toConfig } } = visualizationTypeDefinition;

  return toConfig(formValues);
};

const fromConfig = (config: AggregationWidgetConfig) => ({ visualization: { type: config.visualization } });
const toConfig = (formValues: WidgetConfigFormValues, currentConfig: AggregationWidgetConfig) => currentConfig
  .toBuilder()
  .visualization(formValues.visualization.type)
  .visualizationConfig(formValuesToVisualizationConfig(formValues.visualization.type, formValues.visualization.config))
  .build();

const VisualizationElement: AggregationElement = {
  title: 'Visualization',
  key: 'visualization',
  order: 4,
  allowCreate: (formValues: WidgetConfigFormValues) => isEmpty(formValues.visualization),
  component: VisualizationConfiguration,
  fromConfig,
  toConfig,
};

export default VisualizationElement;
