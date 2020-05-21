// @flow strict
import colorLevel from './colorLevel';
import contrastingColor from './contrastingColor';
import opacify from './opacify';
import readableColor from './readableColor';

export type Utils = {
  colorLevel: mixed,
  contrastingColor: mixed,
  opacify: mixed,
  readableColor: mixed,
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
