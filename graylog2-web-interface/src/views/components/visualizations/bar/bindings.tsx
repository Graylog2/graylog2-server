import * as React from 'react';
import type { VisualizationType } from 'views/types';

import BarVisualization from 'views/components/visualizations/bar/BarVisualization';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import { BarVisualizationConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

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
