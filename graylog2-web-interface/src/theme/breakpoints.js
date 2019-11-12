const min = {
  xs: '480px',
  sm: '768px',
  md: '992px',
  lg: '1200px',
};

const max = {};

Object.keys(min).forEach((bp) => {
  max[bp] = min[bp];
});

const breakpoints = {
  min: { ...min },
  max: { ...max },
};

export default breakpoints;
