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
import PieVisualization from 'views/components/visualizations/pie/PieVisualization';
import { hasAtLeastOneMetric } from 'views/components/visualizations/validations';

const validate = hasAtLeastOneMetric('vis_validation.pie_chart_requires_metric');

const pieChart: VisualizationType<typeof PieVisualization.type> = {
  type: PieVisualization.type,
  get displayName() {
    return visT('vis_types.pie_chart');
  },
  component: PieVisualization,
  validate,
};

export default pieChart;
