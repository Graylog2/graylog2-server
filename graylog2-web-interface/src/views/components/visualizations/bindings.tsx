import type { VisualizationType } from 'views/types';

import dataTable from 'views/components/datatable/bindings';

import areaChart from './area/bindings';
import barChart from './bar/bindings';
import heatmap from './heatmap/bindings';
import lineChart from './line/bindings';
import singleNumber from './number/bindings';
import pieChart from './pie/bindings';
import scatterChart from './scatter/bindings';
import worldMap from './worldmap/bindings';

const visualizationBindings: Array<VisualizationType> = [
  areaChart,
  barChart,
  dataTable,
  heatmap,
  lineChart,
  singleNumber,
  pieChart,
  scatterChart,
  worldMap,
];

export default visualizationBindings;
