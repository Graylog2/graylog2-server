import chroma from 'chroma-js';

const brand = {
  primary: '#ff3633',
  secondary: '#fff',
  tertiary: '#1f1f1f',
};

const global = {
  textDefault: '#1f1f1f',
  textAlt: '#fff',
  background: '#e8e8e8',
  contentBackground: '#fff',
  link: '#702785',
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
};

const variantLight = {};
const variantDark = {};

Object.keys(variant).forEach((name) => {
  variantLight[name] = chroma.mix(variant[name], gray[100], 0.5).hex();
  variantDark[name] = chroma.mix(variant[name], gray[10], 0.75).hex();
});

const teinte = {
  brand,
  global,
  gray,
  variant: {
    ...variant,
    light: { ...variantLight },
    dark: { ...variantDark },
  },
};

export default teinte;
