// @flow strict
import { type Breakpoints } from './breakpoints';
import { type Colors } from './colors';
import { type Utils } from './utils';

export type {
  Breakpoints,
  Colors,
  Utils,
};

export type ThemeInterface = {
  breakpoints: Breakpoints,
  colors: Colors,
  utils: Utils,
};
