import chroma from 'chroma-js';

import { darken, lighten, lightThemeRatio } from './util';

const brand = {
  primary: '#ff3633',
  secondary: '#fff',
  tertiary: '#1f1f1f',
};

const global = {
  background: '#d4d4d4',
  contentBackground: '#fff',
  link: '#fc3b67',
  textAlt: '#fff',
  textDefault: '#585c72',
};

global.linkHover = chroma(global.link).brighten(1).hex();

const grayScale = chroma.scale([global.textDefault, global.textAlt]).colors(10);
const gray = {};

grayScale.forEach((tint, index) => {
  const key = (index + 1) * 10;

  gray[key] = tint;
});

const variant = {
  danger: '#bc2c4c',
  default: '#f91f9d',
  info: '#fc3b67',
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

// '#f9fcfc'
// '#333333'
// '#d4d4d4'
// '#585c72'
// '#fc3b67'
// '#bc2c4c'

Object.keys(variant).forEach((name) => {
  if (typeof variant[name] === 'string') {
    variant.light[name] = lighten(variant[name], lightThemeRatio[0]);
    variant.lighter[name] = lighten(variant[name], lightThemeRatio[1]);
    variant.lightest[name] = lighten(variant[name], lightThemeRatio[2]);

    variant.dark[name] = darken(variant[name], lightThemeRatio[0]);
    variant.darker[name] = darken(variant[name], lightThemeRatio[1]);
    variant.darkest[name] = darken(variant[name], lightThemeRatio[2]);
  }
});

const table = {
  background: lighten(variant.default, 0.95),
  backgroundAlt: lighten(variant.default, 0.85),
  backgroundHover: lighten(variant.default, 0.9),
  variant: {
    danger: lighten(variant.danger, 0.75),
    active: lighten(variant.default, 0.75),
    info: lighten(variant.info, 0.75),
    primary: lighten(variant.primary, 0.75),
    success: lighten(variant.success, 0.75),
    warning: lighten(variant.warning, 0.75),
  },
  variantHover: {
    danger: variant.lighter.danger,
    active: variant.lighter.default,
    info: variant.lighter.info,
    primary: variant.lighter.primary,
    success: variant.lighter.success,
    warning: variant.lighter.warning,
  },
};

const input = {
  background: global.contentBackground,
  backgroundDisabled: darken(global.contentBackground, 0.25),
  border: variant.light.default,
  borderFocus: variant.light.info,
  boxShadow: `inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 8px ${chroma(variant.dark.info).alpha(0.4).css()}`,
  color: global.textDefault,
  colorDisabled: gray[60],
  placeholder: gray[60],
};

/* eslint-disable prefer-destructuring */
global.navigationBackground = global.contentBackground;
global.navigationBoxShadow = chroma(variant.lightest.default).alpha(0.5).css();
/* eslint-enable prefer-destructuring */

const partyGorillaLight = {
  brand,
  global,
  gray,
  input,
  table,
  variant,
};

export default partyGorillaLight;
