// @flow strict
import colorLevel from './colorLevel';
import contrastingColor from './contrastingColor';
import opacify, { type Opacify } from './opacify';
import readableColor, { type ReadableColor } from './readableColor';

export type Utils = {
  colorLevel: any,
  contrastingColor: any,
  opacify: Opacify,
  readableColor: ReadableColor,
};

const utils: Utils = {
  colorLevel,
  contrastingColor,
  opacify,
  readableColor,
};

export default utils;
export {
  colorLevel,
  contrastingColor,
  opacify,
  readableColor,
};
