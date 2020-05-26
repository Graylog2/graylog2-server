// @flow strict
import chroma from 'chroma-js';

export type Opacify = {
  (string, number): string,
};

function opacify(color: string, amount: number): string {
  /**
   * Increases the opacity of a color. Its range for the amount is between 0 to 1.
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {number} amount - any positive number
   */

  if (color === 'transparent') {
    return color;
  }

  const parsedAlpha = chroma(color).alpha();
  const newAlpha = (parsedAlpha * 100 + parseFloat(amount) * 100) / 100;

  return chroma(color).alpha(newAlpha).css();
}

export default opacify;
