
import { readableColor as polishedColor } from 'polished';
import colors from '../colors';

export default function readableColor(color, darkColor = colors.global.textDefault, lightColor = colors.global.textAlt) {
  /**
   * Recreating `color-level` from Bootstrap's SCSS functions
   * https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {string} darkColor - defaults to theme's darkest gray
   * @param {string} lightColor - defaults to theme's lightest gray
   */

  return polishedColor(color, darkColor, lightColor);
}
