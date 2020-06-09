// @flow strict
import colors from './colors';
import breakpoints from './breakpoints';
import fonts from './fonts';
import utils from './utils';
import type { ThemeInterface } from './types';

const theme: ThemeInterface = {
  colors,
  breakpoint: {
    ...breakpoints,
  },
  utils,
  fonts,
};

const themeModes: Array<string> = ['teinte'];

export default theme;

export {
  breakpoints as breakpoint,
  colors,
  fonts,
  utils,
  themeModes,
};

export type { ThemeInterface };
