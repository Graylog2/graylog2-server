import colors from './colors'; // TODO: this will need to be modified by the user in the future
import breakpoints from './breakpoints';
import utils from './utils';

const theme = {
  color: {
    ...colors,
  },
  breakpoint: {
    ...breakpoints,
  },
  util: {
    ...utils,
  },
};

export default theme;

export {
  breakpoints as breakpoint,
  colors as color,
  utils as util,
};
