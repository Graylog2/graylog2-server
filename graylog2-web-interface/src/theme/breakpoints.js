// @flow strict
const sizes: {[string]: number} = {
  xs: 480,
  sm: 768,
  md: 992,
  lg: 1200,
};

const min: {[string]: string} = {};
const max: {[string]: string} = {};

Object.keys(sizes).forEach((bp) => {
  min[bp] = `${sizes[bp]}px`;
  max[bp] = `${sizes[bp] - 1}px`;
});

export type Breakpoints = {
  min: {
    xs: string,
    sm: string,
    md: string,
    lg: string,
  },
  max: {
    xs: string,
    sm: string,
    md: string,
    lg: string,
  },
};

const breakpoints: Breakpoints = {
  min,
  max,
};

export default breakpoints;
