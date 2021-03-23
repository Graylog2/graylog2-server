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
import * as React from 'react';
import type { VisualizationType } from 'views/types';

import BarVisualization from 'views/components/visualizations/bar/BarVisualization';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';

type BarVisualizationConfigFormValues = {
  barmode: 'group' | 'stack' | 'relative' | 'overlay',
};

const barChart: VisualizationType = {
  type: BarVisualization.type,
  displayName: 'Bar Chart',
  component: BarVisualization,
  config: {
    fromConfig: (config: BarVisualizationConfig | undefined): BarVisualizationConfigFormValues => ({ barmode: config?.barmode ?? 'group' }),
    toConfig: (formValues: BarVisualizationConfigFormValues): BarVisualizationConfig => BarVisualizationConfig.create(formValues.barmode),
    fields: [{
      name: 'barmode',
      title: 'Mode',
      type: 'select',
      options: [['Group', 'group'], ['Stack', 'stack'], ['Relative', 'relative'], ['Overlay', 'overlay']],
      required: true,
      helpComponent: () => {
        const options = {
          group: {
            label: 'Group',
            help: 'Every series is represented by its own bar in the chart.',
          },
          stack: {
            label: 'Stack',
            help: 'All series are stacked upon each other resulting in one bar.',
          },
          relative: {
            label: 'Relative',
            help: 'All series are stacked upon each other resulting in one chart. But negative series are placed below zero.',
          },
          overlay: {
            label: 'Overlay',
            help: 'All series are placed as bars upon each other. To be able to see the bars the opacity is reduced to 75%.'
              + ' It is recommended to use this option with not more than 3 series.',
          },
        };

        return (
          <ul>
            {Object.values(options).map(({ label, help }) => (
              <li key={label}><h4>{label}</h4>
                {help}
              </li>
            ))}
          </ul>
        );
      },
    }],
  },
};

export default barChart;
