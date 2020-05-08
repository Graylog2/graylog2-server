import chroma from 'chroma-js';

const brand = {
  primary: '#ff3633',
  secondary: '#fff',
  tertiary: '#1f1f1f',
};

const global = {
  background: '#e8e8e8',
  contentBackground: '#fff',
  link: '#702785',
  textAlt: '#fff',
  textDefault: '#1f1f1f',
};

global.linkHover = chroma(global.link).darken(1).hex();

const grayScale = chroma.scale([global.textDefault, global.textAlt]).colors(10);
const gray = {};
grayScale.forEach((tint, index) => {
  const key = (index + 1) * 10;
  gray[key] = tint;
});

const variant = {
  danger: '#ad0707',
  default: '#1f1f1f',
  info: '#0063be',
  primary: '#702785',
  success: '#00ae42',
  warning: '#ffd200',
  lightest: {},
  lighter: {},
  light: {},
  dark: {},
  darker: {},
  darkest: {},
};

Object.keys(variant).forEach((name) => {
  if (typeof variant[name] === 'string') {
    variant.light[name] = chroma.mix(variant[name], '#fff', 0.15).hex();
    variant.lighter[name] = chroma.mix(variant[name], '#fff', 0.5).hex();
    variant.lightest[name] = chroma.mix(variant[name], '#fff', 0.85).hex();

    variant.dark[name] = chroma.mix(variant[name], '#000', 0.15).hex();
    variant.darker[name] = chroma.mix(variant[name], '#000', 0.5).hex();
    variant.darkest[name] = chroma.mix(variant[name], '#000', 0.85).hex();
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
