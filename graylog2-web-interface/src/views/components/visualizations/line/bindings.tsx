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

type LineVisualizationConfigFormValues = {
  interpolation: 'linear' | 'step-after' | 'spline';
};

const validate = hasAtLeastOneMetric('Line chart');

const lineChart: VisualizationType<LineVisualizationConfig, LineVisualizationConfigFormValues> = {
  type: LineVisualization.type,
  displayName: 'Line Chart',
  component: LineVisualization,
  config: {
    createConfig: () => ({ interpolation: LineVisualizationConfig.DEFAULT_INTERPOLATION }),
    fromConfig: (config: LineVisualizationConfig | undefined) => ({ interpolation: config?.interpolation ?? LineVisualizationConfig.DEFAULT_INTERPOLATION }),
    toConfig: (formValues: LineVisualizationConfigFormValues) => LineVisualizationConfig.create(formValues.interpolation),
    fields: [{
      name: 'interpolation',
      title: 'Interpolation',
      type: 'select',
      options: ['linear', 'step-after', 'spline'],
      required: true,
    }],
  },
  capabilities: ['event-annotations'],
  validate,
};

export default lineChart;
