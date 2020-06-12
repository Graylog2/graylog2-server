// @flow strict

import { type Modes } from './colors';
import { type Fonts } from './fonts';
import { type Utils } from './utils';

export type ThemeInterface = {
  colors: Modes,
  fonts: Fonts,
  utils: Utils,
};
