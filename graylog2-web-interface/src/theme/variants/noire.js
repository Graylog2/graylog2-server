import chroma from 'chroma-js';
import teinte from './teinte';

const desaturate = color => chroma(color).desaturate(2).css();

const brand = {
  primary: desaturate(teinte.brand.primary),
  secondary: desaturate(teinte.brand.secondary),
  tertiary: desaturate(teinte.brand.tertiary),
};

const global = {
  textDefault: desaturate(teinte.global.textDefault),
  textAlt: desaturate(teinte.global.textAlt),
  background: desaturate(teinte.global.background),
  contentBackground: desaturate(teinte.global.contentBackground),
  link: desaturate(teinte.global.link),
  linkHover: desaturate(teinte.global.linkHover),
};

const grayScale = chroma.scale([global.textDefault, global.textAlt]).colors(10);
const gray = {};
grayScale.forEach((tint, index) => {
  const key = (index + 1) * 10;
  gray[key] = tint;
});

const variant = {
  danger: desaturate(teinte.variant.danger),
  default: desaturate(teinte.variant.default),
  info: desaturate(teinte.variant.info),
  primary: desaturate(teinte.variant.primary),
  success: desaturate(teinte.variant.success),
  warning: desaturate(teinte.variant.warning),
};

const variantLight = {};
const variantDark = {};

Object.keys(variant).forEach((name) => {
  variantLight[name] = chroma(variant[name]).brighten(1).hex();
  variantDark[name] = chroma(variant[name]).darken(1.5).hex();
});

const noire = {
  brand,
  global,
  gray,
  variant: {
    ...variant,
    light: { ...variantLight },
    dark: { ...variantDark },
  },
};

export default noire;
