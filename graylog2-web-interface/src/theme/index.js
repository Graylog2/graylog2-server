// @flow strict
import PropTypes from 'prop-types';

import breakpoints, { breakpointPropTypes } from './breakpoints';
import colors, { colorsPropTypes } from './colors';
import fonts, { fontsPropTypes } from './fonts';
import utils from './utils';
import type { ThemeInterface } from './types';

const themePropTypes = PropTypes.shape({
  breakpoints: breakpointPropTypes.isRequired,
  colors: colorsPropTypes.isRequired,
  fonts: fontsPropTypes.isRequired,
  utils: PropTypes.object,
});

export {
  breakpoints,
  colors,
  fonts,
  utils,
  themePropTypes,
};

export type { ThemeInterface };
