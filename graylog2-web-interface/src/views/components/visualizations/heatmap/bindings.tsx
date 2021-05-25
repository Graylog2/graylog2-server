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
import type { VisualizationType } from 'views/types';

import HeatmapVisualization from 'views/components/visualizations/heatmap/HeatmapVisualization';
import HeatmapVisualizationConfig, { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';
import { defaultCompare } from 'views/logic/DefaultCompare';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import {
  areAtLeastNGroupingsConfigured,
  areAtLeastNMetricsConfigured,
} from 'views/components/visualizations/validations';

type HeatMapVisualizationConfigFormValues = {
  colorScale: typeof COLORSCALES[number],
  reverseScale: boolean,
  autoScale: boolean,
  zMin: number,
  zMax: number
  useSmallestAsDefault: boolean,
  defaultValue: number,
};

const validate = (formValues: WidgetConfigFormValues) => {
  const errors = [];

  if (!areAtLeastNGroupingsConfigured(formValues, 2)) {
    errors.push('Heatmap requires at least two groupings.');
  } else {
    const groupingDirections = formValues.groupBy.groupings.map((grouping) => grouping.direction);

    if (!groupingDirections.includes('row') || !groupingDirections.includes('column')) {
      errors.push('Groupings must include row and column groupings.');
    }
  }

  if (!areAtLeastNMetricsConfigured(formValues, 1)) {
    errors.push('At least one metric must be configured.');
  }

  return errors.length > 0
    ? { type: errors.join(' ') }
    : {};
};

const heatmap: VisualizationType<HeatmapVisualizationConfig, HeatMapVisualizationConfigFormValues> = {
  type: HeatmapVisualization.type,
  displayName: 'Heatmap',
  component: HeatmapVisualization,
  config: {
    fromConfig: ({ autoScale, colorScale, reverseScale, defaultValue, useSmallestAsDefault, zMax, zMin }: HeatmapVisualizationConfig) => ({
      autoScale, colorScale, reverseScale, defaultValue, useSmallestAsDefault, zMax, zMin,
    }),
    toConfig: ({ autoScale = false, colorScale, reverseScale = false, useSmallestAsDefault, zMax, zMin, defaultValue }: HeatMapVisualizationConfigFormValues) => HeatmapVisualizationConfig
      .create(colorScale, reverseScale, autoScale, zMin, zMax, useSmallestAsDefault, defaultValue),
    createConfig: () => ({ colorScale: 'Viridis', autoScale: true }),
    fields: [{
      name: 'colorScale',
      title: 'Color Scale',
      required: true,
      type: 'select',
      options: [...COLORSCALES].sort(defaultCompare),
    }, {
      name: 'reverseScale',
      type: 'boolean',
      title: 'Reverse Scale',
    }, {
      name: 'autoScale',
      type: 'boolean',
      title: 'Auto Scale',
    }, {
      name: 'zMin',
      type: 'numeric',
      title: 'Min',
      required: true,
      isShown: (values: HeatMapVisualizationConfigFormValues) => !values?.autoScale,
    }, {
      name: 'zMax',
      type: 'numeric',
      title: 'Max',
      required: true,
      isShown: (values: HeatMapVisualizationConfigFormValues) => !values?.autoScale,
    }, {
      name: 'useSmallestAsDefault',
      type: 'boolean',
      title: 'Use smallest as default',
    }, {
      name: 'defaultValue',
      type: 'numeric',
      title: 'Default Value',
      isShown: (values: HeatMapVisualizationConfigFormValues) => !values?.useSmallestAsDefault,
      required: false,
    }],
  },
  validate,
};

export default heatmap;
