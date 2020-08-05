import chroma from 'chroma-js';

export const lightThemeRatio = ['0.22', '0.55', '0.88'];
export const darkThemeRatio = ['0.15', '0.55', '0.95'];

function lighten(color, ratio) { return chroma.mix(color, '#fff', ratio).hex(); }
function darken(color, ratio) { return chroma.mix(color, '#000', ratio).hex(); }

export {
  darken,
  lighten,
};
