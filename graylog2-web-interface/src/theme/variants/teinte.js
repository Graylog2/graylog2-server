import { darken, lighten, tint } from 'polished';

const brand = {
  primary: '#ff3633',
  secondary: '#fff',
  tertiary: '#1f1f1f',
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
  contentBackground: '#fff',
  link: '#702785',
  linkHover: darken(0.10, '#702785'),
};

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
