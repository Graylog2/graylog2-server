// @flow strict
export type InterpolationMode = 'linear' | 'step-after' | 'spline';

const toPlotly = (value: InterpolationMode) => {
  switch (value) {
    case 'step-after': return 'hv';
    default: return value;
  }
};

export default toPlotly;
