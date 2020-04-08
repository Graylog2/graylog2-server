import chroma from 'chroma-js';
import { memoize } from 'lodash';
import { teinte } from 'theme/colors';

const readableColor = memoize((hex,
  darkColor = teinte.global.textDefault,
  lightColor = teinte.global.textAlt) => {
  /**
   * Returns `textDefault` or `textAlt` (or optional light and dark return colors) for best contrast depending on the luminosity of the given color. Follows [W3C specs for readability](https://www.w3.org/TR/WCAG20-TECHS/G18.html).
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {string} darkColor - defaults to theme's darkest gray
   * @param {string} lightColor - defaults to theme's lightest gray
   */

  const luminanceRatio = 0.179;

  return chroma(hex).luminance() < luminanceRatio ? lightColor : darkColor;
});

export default readableColor;
