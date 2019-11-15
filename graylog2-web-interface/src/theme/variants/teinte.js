import { darken, lighten, tint } from 'polished';

const brand = {
  primary: '#FF3633',
  secondary: '#FFF',
  tertiary: '#1F1F1F',
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
  contentBackground: '#fff',
  link: '#702785',
  linkHover: darken(0.10, '#702785'),
};

const variant = {
  danger: '#AD0707',
  default: '#1F1F1F',
  info: '#0063BE',
  primary: '#702785',
  success: '#00AE42',
  warning: '#FFD200',
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
