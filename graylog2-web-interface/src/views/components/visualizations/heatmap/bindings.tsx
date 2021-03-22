import type { VisualizationType } from 'views/types';

import HeatmapVisualization from 'views/components/visualizations/heatmap/HeatmapVisualization';
import HeatmapVisualizationConfig, { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';
import { HeatMapVisualizationConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

const heatmap: VisualizationType = {
  type: HeatmapVisualization.type,
  displayName: 'Heatmap',
  component: HeatmapVisualization,
  config: {
    fromConfig: ({ autoScale, colorScale, reverseScale, defaultValue, useSmallestAsDefault, zMax, zMin }: HeatmapVisualizationConfig): HeatMapVisualizationConfigFormValues => ({
      autoScale, colorScale, reverseScale, defaultValue, useSmallestAsDefault, zMax, zMin,
    }),
    toConfig: ({ autoScale = false, colorScale, reverseScale = false, useSmallestAsDefault, zMax, zMin, defaultValue }: HeatMapVisualizationConfigFormValues) => HeatmapVisualizationConfig
      .create(colorScale, reverseScale, autoScale, zMin, zMax, useSmallestAsDefault, defaultValue),
    fields: [{
      name: 'colorScale',
      title: 'Color Scale',
      required: true,
      type: 'select',
      options: COLORSCALES,
    }, {
      name: 'reverseScale',
      type: 'boolean',
      title: 'Reverse Scale',
    }, {
      name: 'autoScale',
      type: 'boolean',
      title: 'Auto Scale',
    }, {
      name: 'zMin',
      type: 'numeric',
      title: 'Min',
      required: true,
    }, {
      name: 'zMax',
      type: 'numeric',
      title: 'Max',
      required: true,
    }, {
      name: 'useSmallestAsDefault',
      type: 'boolean',
      title: 'Use smallest as default',
    }, {
      name: 'defaultValue',
      type: 'numeric',
      title: 'Default Value',
      isShown: (values: HeatMapVisualizationConfigFormValues) => !values?.useSmallestAsDefault,
      required: true,
    }],
  },
};

export default heatmap;
