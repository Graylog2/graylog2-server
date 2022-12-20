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
import LineVisualization from 'views/components/visualizations/line/LineVisualization';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import { hasAtLeastOneMetric } from 'views/components/visualizations/validations';
import type { AxisType } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { DEFAULT_AXIS_TYPE, axisTypes } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import type { InterpolationType } from 'views/Constants';
import { DEFAULT_INTERPOLATION, interpolationTypes } from 'views/Constants';

type LineVisualizationConfigFormValues = {
  interpolation: InterpolationType;
  axisType: AxisType;
};

const validate = hasAtLeastOneMetric('Line chart');

const lineChart: VisualizationType<typeof LineVisualization.type, LineVisualizationConfig, LineVisualizationConfigFormValues> = {
  type: LineVisualization.type,
  displayName: 'Line Chart',
  component: LineVisualization,
  config: {
    createConfig: () => ({ interpolation: DEFAULT_INTERPOLATION, axisType: DEFAULT_AXIS_TYPE }),
    fromConfig: (config: LineVisualizationConfig | undefined) => ({
      interpolation: config?.interpolation ?? DEFAULT_INTERPOLATION,
      axisType: config?.axisType ?? DEFAULT_AXIS_TYPE,
    }),
    toConfig: (formValues: LineVisualizationConfigFormValues) => LineVisualizationConfig.create(formValues.interpolation, formValues.axisType),
    fields: [{
      name: 'interpolation',
      title: 'Interpolation',
      type: 'select',
      options: interpolationTypes,
      required: true,
    }, {
      name: 'axisType',
      title: 'Axis Type',
      type: 'select',
      options: axisTypes,
      required: true,
    }],
  },
  capabilities: ['event-annotations'],
  validate,
};

export default lineChart;
