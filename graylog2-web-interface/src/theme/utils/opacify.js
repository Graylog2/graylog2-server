import chroma from 'chroma-js';
import { memoize } from 'lodash';

const opacify = memoize(({ color, level }) => {
  /**
   * Increases the opacity of a color. Its range for the level is between 0 to 1.
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {number} level - any positive number
   */

  if (color === 'transparent') {
    return color;
  }

  const parsedAlpha = chroma(color).alpha();
  const newAlpha = (parsedAlpha * 100 + parseFloat(level) * 100) / 100;

  return chroma(color).alpha(newAlpha).css();
});

export default opacify;
