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
import type { ArrayElement } from 'views/types';
import type { DEFAULT_AXIS_KEY } from 'views/components/visualizations/Constants';

export const axisTypes = ['linear', 'logarithmic'] as const;

export type AxisType = ArrayElement<typeof axisTypes>;

export type ChartAxisConfig = {
  [DEFAULT_AXIS_KEY]?: { title?: string; color: string };
  percent?: { title?: string; color?: string };
  time?: { title?: string; color?: string };
  size?: { title?: string; color?: string };
  xaxis?: { title?: string; color?: string };
};
export type XYVisualizationConfigFormValues = {
  showAxisLabels: boolean;
  axisConfig: ChartAxisConfig;
};

export interface XYVisualization {
  axisType: AxisType;
  axisConfig: ChartAxisConfig;
}

export const DEFAULT_AXIS_TYPE: AxisType = 'linear';

export const DEFAULT_AXIS_CONFIG = {};
