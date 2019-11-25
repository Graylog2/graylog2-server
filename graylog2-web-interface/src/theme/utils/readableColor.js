
import { readableColor as polishedColor } from 'polished';
import teinte from '../variants/teinte'; // TODO: replace this with whatever is coming from ThemeProvider

export default function readableColor(color, darkColor = teinte.global.textDefault, lightColor = teinte.global.textAlt) {
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
