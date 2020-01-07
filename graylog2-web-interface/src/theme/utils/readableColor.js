import { readableColor as polishedColor } from 'polished';

import teinte from '../variants/teinte'; // TODO: replace this with whatever is coming from ThemeProvider

export default function readableColor(color, darkColor = teinte.global.textDefault, lightColor = teinte.global.textAlt) {
  /**
   * Generating a readable color following W3C specs using [polished](https://polished.js.org/docs/#readablecolor)
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {string} darkColor - defaults to theme's darkest gray
   * @param {string} lightColor - defaults to theme's lightest gray
   */

  return polishedColor(color, darkColor, lightColor);
}
