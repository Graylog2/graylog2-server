import type { VisualizationType } from 'views/types';

import ScatterVisualization from 'views/components/visualizations/scatter/ScatterVisualization';

const scatterChart: VisualizationType = {
  type: ScatterVisualization.type,
  displayName: 'Scatter Plot',
  component: ScatterVisualization,
};

export default scatterChart;
