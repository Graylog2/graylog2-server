
import { mix } from 'polished';
import colors from './colors';

export default function colorLevel(colorHex, level = 0) {
  /**
   * Recreating `color-level` from Bootstrap's SCSS functions
   * https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {number} level - any positive or negative number
   */
  const colorBase = level > 0 ? colors.global.textDefault : colors.global.textAlt;
  const absLevel = Math.abs(level) * 0.08; // TODO: make 8% a theme variable

  return mix(absLevel, colorBase, colorHex);
}
