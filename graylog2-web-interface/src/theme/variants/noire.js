import chroma from 'chroma-js';

import { darken, lighten } from './util';

const brand = {
  primary: '#ff3633',
  secondary: '#fff',
  tertiary: '#1f1f1f',
};

const global = {
  background: '#222',
  contentBackground: '#303030',
  link: '#00bc8c',
  textAlt: '#888',
  textDefault: '#fff',
};

global.linkHover = chroma(global.link).darken(1).hex();

const grayScale = chroma.scale([global.textDefault, global.textAlt]).colors(10);
const gray = {};

grayScale.forEach((tint, index) => {
  const key = (index + 1) * 10;

  gray[key] = tint;
});

const variant = {
  danger: '#E74C3C',
  default: '#adb5bd',
  info: '#3498DB',
  primary: '#375a7f',
  success: '#00bc8c',
  warning: '#F39C12',
  lightest: {},
  lighter: {},
  light: {},
  dark: {},
  darker: {},
  darkest: {},
};

Object.keys(variant).forEach((name) => {
  if (typeof variant[name] === 'string') {
    variant.light[name] = darken(variant[name], 0.15);

    variant.lighter[name] = darken(variant[name], 0.5);

    variant.lightest[name] = darken(variant[name], 0.85);

    variant.dark[name] = lighten(variant[name], 0.15);

    variant.darker[name] = lighten(variant[name], 0.5);

    variant.darkest[name] = lighten(variant[name], 0.85);
  }
});

/* eslint-disable prefer-destructuring */
global.tableBackground = gray[100];

global.tableBackgroundAlt = gray[80];

global.inputBackground = global.contentBackground;
/* eslint-enable prefer-destructuring */

const teinte = {
  brand,
  global,
  gray,
  variant,
};

export default teinte;
