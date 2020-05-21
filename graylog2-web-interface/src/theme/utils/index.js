// @flow strict
import colorLevel from './colorLevel';
import contrastingColor from './contrastingColor';
import opacify from './opacify';
import readableColor from './readableColor';

export type Utils = {
  colorLevel: any,
  contrastingColor: any,
  opacify: any,
  readableColor: any,
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
