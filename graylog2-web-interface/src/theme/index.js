// @flow
import teinte from './teinte';
import breakpoints from './breakpoints';
import util from './util';

const theme = {
  teinte,
  breakpoint: {
    ...breakpoints,
  },
  util,
};

const themeModes = ['teinte', 'noire'];

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
  teinte,
  themeModes,
  util,
};
