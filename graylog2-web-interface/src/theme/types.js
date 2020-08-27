// @flow strict

import type { Colors } from './colors';
import type { Fonts } from './fonts';
import type { Utils } from './utils';
import type { Breakpoints } from './breakpoints';

export type ThemeInterface = {
  breakpoints: Breakpoints,
  colors: Colors,
  fonts: Fonts,
  utils: Utils,
  mode: string,
  changeMode: (string) => void,
};
