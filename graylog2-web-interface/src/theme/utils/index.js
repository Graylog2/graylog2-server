// @flow strict
import PropTypes from 'prop-types';

import colorLevel, { type ColorLevel } from './colorLevel';
import contrastingColor, { type ContrastingColor } from './contrastingColor';
import opacify, { type Opacify } from './opacify';
import readableColor, { type ReadableColor } from './readableColor';

export type Utils = {
  colorLevel: ColorLevel,
  contrastingColor: ContrastingColor,
  opacify: Opacify,
  readableColor: ReadableColor,
};

export const utilsPropTypes = PropTypes.shape({
  colorLevel: PropTypes.func,
  contrastingColor: PropTypes.func,
  opacify: PropTypes.func,
  readableColor: PropTypes.func,
});

const utils = {
  colorLevel,
  contrastingColor,
  opacify,
  readableColor,
};

export {
  colorLevel,
  contrastingColor,
  opacify,
  readableColor,
};

export default utils;
