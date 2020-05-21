// @flow strict
const sizes = {
  xs: 480,
  sm: 768,
  md: 992,
  lg: 1200,
};

const min = {};
const max = {};

Object.keys(sizes).forEach((bp) => {
  min[bp] = `${sizes[bp]}px`;
  max[bp] = `${sizes[bp] - 1}px`;
});

export type Breakpoints = {
  min: { [string]: string },
  max: { [string]: string },
};

const breakpoints: Breakpoints = {
  min,
  max,
};

export default breakpoints;
