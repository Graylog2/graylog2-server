// @flow strict
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
