import Color from 'color';
// `color` Documentation https://github.com/Qix-/color/blob/master/README.md

import teinte from './teinte';

function lighten(hex, percentage) {
  return Color(hex).whiten(percentage).hex();
}

function darken(hex, percentage) {
  return Color(hex).blacken(percentage).hex();
}

function opposite(hex) {
  return Color(hex).rotate(180).hex();
}

export { teinte, lighten, darken, opposite };
