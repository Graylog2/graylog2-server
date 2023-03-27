import { PropTypeBreakpoints, PropTypeColors, PropTypeFonts, PropTypeSpacings, PropTypeUtils } from '@graylog/sawmill';
import PropTypes from 'prop-types';

export type ColorVariants = 'danger' | 'default' | 'info' | 'primary' | 'success' | 'warning';
const ThemePropTypes = PropTypes.shape({
  breakpoints: PropTypeBreakpoints,
  colors: PropTypeColors,
  fonts: PropTypeFonts,
  utils: PropTypeUtils,
  spacings: PropTypeSpacings,
});

export default ThemePropTypes;
