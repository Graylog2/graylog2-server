// @flow strict
import colors from './colors';
import breakpoints from './breakpoints';
import utils from './utils';

const theme = {
  colors,
  breakpoints,
  utils,
};

const themeModes: Array<string> = Object.keys(colors);

export default theme;

export {
  breakpoints,
  colors,
  themeModes,
  utils,
};
