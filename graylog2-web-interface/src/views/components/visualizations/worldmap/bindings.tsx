import type { VisualizationType } from 'views/types';

import WorldMapVisualization from 'views/components/visualizations/worldmap/WorldMapVisualization';

const worldMap: VisualizationType = {
  type: WorldMapVisualization.type,
  displayName: 'World Map',
  component: WorldMapVisualization,
};

export default worldMap;
