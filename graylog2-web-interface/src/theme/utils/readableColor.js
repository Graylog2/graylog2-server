import { readableColor as polishedColor } from 'polished';
import { teinte } from 'theme/colors';

export default function readableColor(hex, darkColor = teinte.global.textDefault, lightColor = teinte.global.textAlt) {
  /**
   * Generating a readable color following W3C specs using [polished](https://polished.js.org/docs/#readablecolor)
   *
   * @param {string} hex - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {string} darkColor - defaults to theme's darkest gray
   * @param {string} lightColor - defaults to theme's lightest gray
   */

  return polishedColor(hex, darkColor, lightColor);
}
