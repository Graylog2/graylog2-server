import {
  lighten,
  darken,
  meetsContrastGuidelines,
} from 'polished';

/**
 * Accepts a color and [WCAG](https://www.w3.org/TR/WCAG21/#distinguishable) level, it then returns a properly contrasting color.
 *
 * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
 * @param {string} [wcagLevel="AA"] -Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")
 *
 * @returns {string}
 *
 */

const contrastingColor = (color, wcagLevel = 'AA') => {
  const mixStep = 0.05;
  let mixture = 0;
  let outputColor = '#000';

  while (mixture < 1) {
    const percent = mixture.toFixed(2);
    const darker = darken(percent, color);
    const lighter = lighten(percent, color);

    if (meetsContrastGuidelines(color, darker)[wcagLevel]) {
      outputColor = darker;
      break;
    }

    if (meetsContrastGuidelines(color, lighter)[wcagLevel]) {
      outputColor = lighter;
      break;
    }

    mixture += mixStep;
  }

  return outputColor;
};

export default contrastingColor;
