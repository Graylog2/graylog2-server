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
