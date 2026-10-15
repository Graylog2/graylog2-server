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
import { translatedOptions, visT } from 'views/components/visualizations/visualizationTranslation';
import type { VisualizationType } from 'views/types';
import AreaVisualization from 'views/components/visualizations/area/AreaVisualization';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import { hasAtLeastOneMetric } from 'views/components/visualizations/validations';
import type { InterpolationType } from 'views/Constants';
import { DEFAULT_INTERPOLATION, interpolationTypes } from 'views/Constants';
import type { AxisType, ChartAxisConfig } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import {
  DEFAULT_AXIS_CONFIG,
  axisTypes,
  DEFAULT_AXIS_TYPE,
} from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import xyAxisConfigFields, { fromAxisConfig } from 'views/components/visualizations/xyAxisConfigFields';

type AreaVisualizationConfigFormValues = {
  interpolation: InterpolationType;
  axisType: AxisType;
  axisConfig: ChartAxisConfig;
  showAxisLabels: boolean;
};

const validate = hasAtLeastOneMetric('vis_validation.area_chart_requires_metric');

const areaChart: VisualizationType<
  typeof AreaVisualization.type,
  AreaVisualizationConfig,
  AreaVisualizationConfigFormValues
> = {
  type: AreaVisualization.type,
  get displayName() {
    return visT('vis_types.area_chart');
  },
  component: AreaVisualization,
  config: {
    createConfig: () => ({
      interpolation: DEFAULT_INTERPOLATION,
      axisType: DEFAULT_AXIS_TYPE,
      axisConfig: DEFAULT_AXIS_CONFIG,
    }),
    fromConfig: (config: AreaVisualizationConfig) => ({
      interpolation: config?.interpolation,
      axisType: config?.axisType,
      ...fromAxisConfig(config),
    }),
    toConfig: (formValues: AreaVisualizationConfigFormValues) =>
      AreaVisualizationConfig.create(
        formValues.interpolation,
        formValues.axisType,
        formValues.showAxisLabels ? formValues.axisConfig : DEFAULT_AXIS_CONFIG,
      ),
    fields: [
      {
        name: 'interpolation',
        get title() {
          return visT('vis_common.interpolation');
        },
        type: 'select',
        get options() {
          return translatedOptions(interpolationTypes, 'vis_common.interpolation_types');
        },
        required: true,
      },
      {
        name: 'axisType',
        get title() {
          return visT('vis_common.axis_type');
        },
        type: 'select',
        get options() {
          return translatedOptions(axisTypes, 'vis_common.axis_types');
        },
        required: true,
      },
      ...xyAxisConfigFields,
    ],
  },
  capabilities: ['event-annotations'],
  validate,
};

export default areaChart;
