import {
  getLuminance,
  meetsContrastGuidelines,
  shade,
  tint,
} from 'polished';

/**
 * Accepts a color and [WCAG](https://www.w3.org/TR/WCAG21/#distinguishable) level, it then returns a properly contrasting color.
 *
 * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
 * @param {string} [wcagLevel="AAA"] -Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")
 *
 * @returns {string}
 *
 */

const contrastingColor = (color, wcagLevel = 'AAA') => {
  const mixStep = 0.05;
  const adjust = getLuminance(color) < 0.5 ? tint : shade;
  let mixture = 0;
  let outputColor = adjust(mixture, color);

  while (mixture <= 1) {
    const percent = mixture.toFixed(2);
    outputColor = adjust(percent, color);

    if (meetsContrastGuidelines(color, outputColor)[wcagLevel]) {
      break;
    }

    mixture += mixStep;
  }

  return outputColor;
};

export default contrastingColor;
