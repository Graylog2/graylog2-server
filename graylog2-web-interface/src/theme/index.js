// @flow strict
import PropTypes from 'prop-types';

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

const themePropTypes = PropTypes.shape({
  breakpoints: PropTypes.object,
  colors: PropTypes.object,
  fonts: PropTypes.object,
  utils: PropTypes.object,
});

export {
  breakpoints,
  colors,
  fonts,
  utils,
  themeModes,
  themePropTypes,
};

export type { ThemeInterface };

export default theme;
