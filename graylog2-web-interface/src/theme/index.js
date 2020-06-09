// @flow strict
import breakpoints from './breakpoints';
import colors from './colors';
import fonts from './fonts';
import utils from './utils';
import type { ThemeInterface } from './types';

const theme: ThemeInterface = {
  breakpoints,
  colors,
  utils,
  fonts,
};

const themeModes: Array<string> = ['teinte'];

export {
  breakpoints,
  colors,
  fonts,
  utils,
  themeModes,
};

export type { ThemeInterface };

export default theme;
