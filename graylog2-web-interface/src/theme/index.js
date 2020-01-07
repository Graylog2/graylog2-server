// @flow strict
import colors from './colors';
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

const themeModes = Object.keys(colors);

export type ThemeInterface = {
  color: {
    primary: { [string]: string },
    secondary: { [string]: string },
    tertiary: { [string]: string },
   },
};

export default theme;

export {
  breakpoints as breakpoint,
  colors as color,
  utils as util,
  themeModes,
};
