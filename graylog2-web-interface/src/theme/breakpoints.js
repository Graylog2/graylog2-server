const sizes = {
  xs: 480,
  sm: 768,
  md: 992,
  lg: 1200,
};

const min = {};
const max = {};

Object.keys(sizes).forEach((bp) => {
  min[bp] = sizes[bp];
  max[bp] = sizes[bp] - 1;
});

const breakpoints = {
  min,
  max,
};

export default breakpoints;
