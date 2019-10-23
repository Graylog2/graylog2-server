import Color from 'color';
// `color` Documentation https://github.com/Qix-/color/blob/master/README.md

import teinte from './teinte';

const percent = value => Math.abs(value / 100);

function darken(hex, percentage) {
  return Color(hex).blacken(percent(percentage)).hex();
}

function mix(hex, level) {
  const mixer = Color(level > 0 ? '#000' : '#fff');
  const color = Color(hex);

  return color.mix(mixer, percent(level)).hex();
}

export { teinte, darken, mix };
