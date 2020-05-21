// @flow strict
import colors from './colors';
import breakpoints from './breakpoints';
import utils from './utils';
import { type ThemeInterface } from './types';

const theme: ThemeInterface = {
  breakpoints,
  colors,
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
