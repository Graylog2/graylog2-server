import chroma from 'chroma-js';

function lighten(color, ratio) { return chroma.mix(color, '#fff', ratio).hex(); }
function darken(color, ratio) { return chroma.mix(color, '#000', ratio).hex(); }

export {
  darken,
  lighten,
};
