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
import LineVisualization from 'views/components/visualizations/line/LineVisualization';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import { hasAtLeastOneMetric } from 'views/components/visualizations/validations';
import type { AxisType, ChartAxisConfig } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import {
  DEFAULT_AXIS_CONFIG,
  DEFAULT_AXIS_TYPE,
  axisTypes,
} from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import type { InterpolationType } from 'views/Constants';
import { DEFAULT_INTERPOLATION, interpolationTypes } from 'views/Constants';
import xyAxisConfigFields, { fromAxisConfig } from 'views/components/visualizations/xyAxisConfigFields';

type LineVisualizationConfigFormValues = {
  interpolation: InterpolationType;
  axisType: AxisType;
  axisConfig: ChartAxisConfig;
  showAxisLabels: boolean;
};

const validate = hasAtLeastOneMetric('vis_validation.line_chart_requires_metric');

const lineChart: VisualizationType<
  typeof LineVisualization.type,
  LineVisualizationConfig,
  LineVisualizationConfigFormValues
> = {
  type: LineVisualization.type,
  get displayName() {
    return visT('vis_types.line_chart');
  },
  component: LineVisualization,
  config: {
    createConfig: () => ({
      interpolation: DEFAULT_INTERPOLATION,
      axisType: DEFAULT_AXIS_TYPE,
      axisConfig: DEFAULT_AXIS_CONFIG,
    }),
    fromConfig: (config: LineVisualizationConfig | undefined) => ({
      interpolation: config?.interpolation ?? DEFAULT_INTERPOLATION,
      axisType: config?.axisType ?? DEFAULT_AXIS_TYPE,
      ...fromAxisConfig(config),
    }),
    toConfig: (formValues: LineVisualizationConfigFormValues) =>
      LineVisualizationConfig.create(
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

export default lineChart;
