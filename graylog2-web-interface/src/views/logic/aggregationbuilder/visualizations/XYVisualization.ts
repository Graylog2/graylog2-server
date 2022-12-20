import assertUnreachable from 'logic/assertUnreachable';

export type AxisType = 'linear' | 'logarithmic';

export interface XYVisualization {
  axisType: AxisType;
}

export type AxisTypeJSON = 'LINEAR' | 'LOGARITHMIC';

export const DEFAULT_AXIS_TYPE: AxisType = 'linear';

export const parseAxisType = (axisType: AxisTypeJSON | null | undefined) => {
  switch (axisType) {
    case 'LINEAR': return 'linear';
    case 'LOGARITHMIC': return 'logarithmic';
    case null:
    case undefined: return DEFAULT_AXIS_TYPE;
    default: return assertUnreachable(axisType, 'Unable to parse axis type: ');
  }
};
