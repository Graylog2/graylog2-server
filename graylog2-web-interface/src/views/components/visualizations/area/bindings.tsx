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
import AreaVisualization from 'views/components/visualizations/area/AreaVisualization';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import { hasAtLeastOneMetric } from 'views/components/visualizations/validations';
import type { InterpolationType } from 'views/Constants';
import { DEFAULT_INTERPOLATION, interpolationTypes } from 'views/Constants';
import type { AxisType } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { axisTypes, DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';

type AreaVisualizationConfigFormValues = {
  interpolation: InterpolationType;
  axisType: AxisType,
};

const validate = hasAtLeastOneMetric('Area chart');

const areaChart: VisualizationType<typeof AreaVisualization.type, AreaVisualizationConfig, AreaVisualizationConfigFormValues> = {
  type: AreaVisualization.type,
  displayName: 'Area Chart',
  component: AreaVisualization,
  config: {
    createConfig: () => ({ interpolation: DEFAULT_INTERPOLATION, axisType: DEFAULT_AXIS_TYPE }),
    fromConfig: (config: AreaVisualizationConfig) => ({ interpolation: config?.interpolation, axisType: config?.axisType }),
    toConfig: (formValues: AreaVisualizationConfigFormValues) => AreaVisualizationConfig.create(formValues.interpolation, formValues.axisType),
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

export default areaChart;
