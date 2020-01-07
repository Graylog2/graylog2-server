import { darken, lighten, tint, invert } from 'polished';
import teinte from './teinte';

const brand = {
  primary: invert(teinte.brand.primary),
  secondary: invert(teinte.brand.secondary),
  tertiary: invert(teinte.brand.tertiary),
};

const gray = {};
const darkestGray = brand.tertiary;
let i = 0;
while (i <= 100) {
  gray[i] = tint((i / 100), darkestGray);
  i += 10;
}

const global = {
  textDefault: gray[0],
  textAlt: gray[100],
  background: gray[90],
  contentBackground: invert(teinte.global.contentBackground),
  link: invert(teinte.global.link),
  linkHover: invert(darken(0.10, teinte.global.linkHover)),
};

const variant = {
  danger: invert(teinte.variant.danger),
  default: invert(teinte.variant.default),
  info: invert(teinte.variant.info),
  primary: invert(teinte.variant.primary),
  success: invert(teinte.variant.success),
  warning: invert(teinte.variant.warning),
};

const variantLight = {};
const variantDark = {};

Object.keys(variant).forEach((name) => {
  variantLight[name] = lighten(0.33, variant[name]);
  variantDark[name] = darken(0.17, variant[name]);
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
