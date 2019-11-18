import { darken, lighten, tint, invert } from 'polished';

const brand = {
  primary: invert('#FF3633'),
  secondary: invert('#FFF'),
  tertiary: invert('#1F1F1F'),
};

const gray = {};
const darkestGray = brand.tertiary;
let i = 0;
while (i < 1) {
  gray[Math.ceil(i * 100)] = tint(i, darkestGray);
  i += 0.10;
}

const global = {
  textDefault: gray[0],
  textAlt: gray[100],
  background: gray[90],
  contentBackground: invert('#fff'),
  link: invert('#702785'),
  linkHover: invert(darken(0.10, '#702785')),
};

const variant = {
  danger: invert('#AD0707'),
  default: invert('#1F1F1F'),
  info: invert('#0063BE'),
  primary: invert('#702785'),
  success: invert('#00AE42'),
  warning: invert('#FFD200'),
};

const variantLight = {};
const variantDark = {};

Object.keys(variant).forEach((name) => {
  variantLight[name] = lighten(0.33, variant[name]);
  variantDark[name] = darken(0.17, variant[name]);
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
