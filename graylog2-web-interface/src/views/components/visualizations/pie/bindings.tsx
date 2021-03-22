import type { VisualizationType } from 'views/types';

import PieVisualization from 'views/components/visualizations/pie/PieVisualization';

const pieChart: VisualizationType = {
  type: PieVisualization.type,
  displayName: 'Pie Chart',
  component: PieVisualization,
};

export default pieChart;
