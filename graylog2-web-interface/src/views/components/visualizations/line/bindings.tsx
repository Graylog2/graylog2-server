import type { VisualizationType } from 'views/types';

import LineVisualization from 'views/components/visualizations/line/LineVisualization';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import { LineVisualizationConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

const lineChart: VisualizationType = {
  type: LineVisualization.type,
  displayName: 'Line Chart',
  component: LineVisualization,
  config: {
    fromConfig: (config: LineVisualizationConfig | undefined): LineVisualizationConfigFormValues => ({ interpolation: config?.interpolation }),
    toConfig: (formValues: LineVisualizationConfigFormValues): LineVisualizationConfig => LineVisualizationConfig.create(formValues.interpolation),
    fields: [{
      name: 'interpolation',
      title: 'Interpolation',
      type: 'select',
      options: ['linear', 'step-after', 'spline'],
      required: true,
    }],
  },
};

export default lineChart;
