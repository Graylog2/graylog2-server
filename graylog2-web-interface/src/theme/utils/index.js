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

export const utilsPropTypes = {
  colorLevel: PropTypes.func.isRequired,
  contrastingColor: PropTypes.func.isRequired,
  opacify: PropTypes.func.isRequired,
  readableColor: PropTypes.func.isRequired,
};

const utils: Utils = {
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
