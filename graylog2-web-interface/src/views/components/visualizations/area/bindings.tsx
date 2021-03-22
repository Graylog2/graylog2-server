import type { VisualizationType } from 'views/types';

import AreaVisualization from 'views/components/visualizations/area/AreaVisualization';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import { AreaVisualizationConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

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
};

export default areaChart;
