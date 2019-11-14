import { darken, lighten, tint } from 'polished';

const brand = {
  primary: '#FF3633',
  secondary: '#FFF',
  tertiary: '#1F1F1F',
};

const grays = {};
const darkestGray = brand.tertiary;
let i = 0;
while (i < 1) {
  grays[Math.ceil(i * 100)] = tint(i, darkestGray);
  i += 0.10;
}

const global = {
  textDefault: grays[0],
  textAlt: grays[100],
  background: grays[90],
  contentBackground: '#fff',
  link: '#702785',
  linkHover: darken(0.10, '#702785'),
};

const variants = {
  danger: '#AD0707',
  default: '#1F1F1F',
  info: '#0063BE',
  primary: '#702785',
  success: '#00AE42',
  warning: '#FFD200',
};

const variantsLight = {};
const variantsDark = {};

Object.keys(variants).forEach((variant) => {
  variantsLight[variant] = lighten(0.33, variants[variant]);
  variantsDark[variant] = darken(0.17, variants[variant]);
});

const colors = {
  // Deprecated Colors
  primary: {
    uno: '#FF3633',
    due: '#FFF',
    tre: '#1F1F1F',
  },
  secondary: {
    uno: '#FF3B00', /* TODO: Replace with #AD0707 */
    due: '#F1F2F2', /* TODO: Replace with #A6AFBD */
    tre: '#DCE1E5',
  },
  tertiary: {
    uno: '#16ACE3', /* TODO: Replace with #0063BE */
    due: '#6DC6E7',
    tre: '#8DC63F', /* TODO: Replace with #00AE42 */
    quattro: '#9E1F63', /* TODO: Replace with #702785 */
    cinque: '#FF6418',
    sei: '#FFD200',
  },
  // New Colors
  brand: { ...brand },
  global: { ...global },
  variant: {
    ...variants,
    light: { ...variantsLight },
    dark: { ...variantsDark },
  },
  gray: { ...grays },
};

export default colors;
