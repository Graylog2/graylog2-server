import chroma from 'chroma-js';

function lighten(color, ratio) { return chroma.mix(color, '#fff', ratio).hex(); }

function darken(color, ratio) { return chroma.mix(color, '#000', ratio).hex(); }

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
  default: lighten(global.textDefault, 0.25),
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
    variant.light[name] = lighten(variant[name], 0.22);
    variant.lighter[name] = lighten(variant[name], 0.55);
    variant.lightest[name] = lighten(variant[name], 0.88);

    variant.dark[name] = darken(variant[name], 0.22);
    variant.darker[name] = darken(variant[name], 0.55);
    variant.darkest[name] = darken(variant[name], 0.88);
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
  boxShadow: `inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 8px ${chroma(variant.light.info).alpha(0.4).css()}`,
  color: global.textDefault,
  colorDisabled: gray[60],
  placeholder: gray[60],
};

const teinte = {
  brand,
  global,
  gray,
  input,
  table,
  variant,
};

export default teinte;
