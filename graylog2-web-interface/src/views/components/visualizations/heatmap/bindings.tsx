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
import { visT } from 'views/components/visualizations/visualizationTranslation';
import type { VisualizationType } from 'views/types';
import HeatmapVisualization from 'views/components/visualizations/heatmap/HeatmapVisualization';
import HeatmapVisualizationConfig, {
  COLORSCALES,
} from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';
import { defaultCompare } from 'logic/DefaultCompare';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import {
  areAtLeastNGroupingsConfigured,
  areAtLeastNMetricsConfigured,
} from 'views/components/visualizations/validations';

type HeatMapVisualizationConfigFormValues = {
  colorScale: (typeof COLORSCALES)[number];
  reverseScale: boolean;
  autoScale: boolean;
  zMin: number;
  zMax: number;
  useSmallestAsDefault: boolean;
  defaultValue: number;
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

  return errors.length > 0 ? { type: errors.join(' ') } : {};
};

const heatmap: VisualizationType<
  typeof HeatmapVisualization.type,
  HeatmapVisualizationConfig,
  HeatMapVisualizationConfigFormValues
> = {
  type: HeatmapVisualization.type,
  get displayName() {
    return visT('vis_types.heatmap');
  },
  component: HeatmapVisualization,
  config: {
    fromConfig: (
      {
        autoScale,
        colorScale,
        reverseScale,
        defaultValue,
        useSmallestAsDefault,
        zMax,
        zMin,
      }: HeatmapVisualizationConfig = HeatmapVisualizationConfig.empty(),
    ) => ({
      autoScale,
      colorScale,
      reverseScale,
      defaultValue,
      useSmallestAsDefault,
      zMax,
      zMin,
    }),
    toConfig: ({
      autoScale = false,
      colorScale,
      reverseScale = false,
      useSmallestAsDefault,
      zMax,
      zMin,
      defaultValue,
    }: HeatMapVisualizationConfigFormValues) => {
      const [finalZMin, finalZMax] = autoScale ? [undefined, undefined] : [zMin, zMax];

      return HeatmapVisualizationConfig.create(
        colorScale,
        reverseScale,
        autoScale,
        finalZMin,
        finalZMax,
        useSmallestAsDefault,
        defaultValue,
      );
    },
    createConfig: () => ({ colorScale: 'Viridis', autoScale: true }),
    fields: [
      {
        name: 'colorScale',
        get title() {
          return visT('vis_common.color_scale');
        },
        required: true,
        type: 'select',
        options: [...COLORSCALES].sort(defaultCompare),
      },
      {
        name: 'reverseScale',
        type: 'boolean',
        get title() {
          return visT('vis_common.reverse_scale');
        },
      },
      {
        name: 'autoScale',
        type: 'boolean',
        get title() {
          return visT('vis_heatmap.auto_scale');
        },
      },
      {
        name: 'zMin',
        type: 'numeric',
        get title() {
          return visT('vis_heatmap.min');
        },
        required: true,
        isShown: (values: HeatMapVisualizationConfigFormValues) => !values?.autoScale,
      },
      {
        name: 'zMax',
        type: 'numeric',
        get title() {
          return visT('vis_heatmap.max');
        },
        required: true,
        isShown: (values: HeatMapVisualizationConfigFormValues) => !values?.autoScale,
      },
      {
        name: 'useSmallestAsDefault',
        type: 'boolean',
        get title() {
          return visT('vis_heatmap.use_smallest_as_default');
        },
      },
      {
        name: 'defaultValue',
        type: 'numeric',
        get title() {
          return visT('vis_heatmap.default_value');
        },
        isShown: (values: HeatMapVisualizationConfigFormValues) => !values?.useSmallestAsDefault,
        required: false,
      },
    ],
  },
  validate,
};

export default heatmap;
