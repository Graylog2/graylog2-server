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

type AreaVisualizationConfigFormValues = {
  interpolation: 'linear' | 'step-after' | 'spline';
};

const validate = hasAtLeastOneMetric('Area chart');

const areaChart: VisualizationType = {
  type: AreaVisualization.type,
  displayName: 'Area Chart',
  component: AreaVisualization,
  config: {
    fromConfig: (config: AreaVisualizationConfig): AreaVisualizationConfigFormValues => ({ interpolation: config.interpolation }),
    toConfig: (formValues: AreaVisualizationConfigFormValues): AreaVisualizationConfig => AreaVisualizationConfig.create(formValues.interpolation),
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

export default areaChart;
