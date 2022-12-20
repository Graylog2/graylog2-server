import type { ArrayElement } from 'views/types';

export const axisTypes = ['linear', 'logarithmic'] as const;

export type AxisType = ArrayElement<typeof axisTypes>;

export interface XYVisualization {
  axisType: AxisType;
}

export const DEFAULT_AXIS_TYPE: AxisType = 'linear';
