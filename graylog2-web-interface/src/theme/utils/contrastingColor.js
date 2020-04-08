import chroma from 'chroma-js';
import { memoize } from 'lodash';

/**
 * Accepts an object containig color and [WCAG distinguishable level](https://www.w3.org/TR/WCAG21/#distinguishable), it then returns a properly contrasting color.
 *
 * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
 * @param {string} [level="AAA"] - Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")
 *
 * @returns {string}
 *
 */

const contrastRatios = {
  AA: 4.5, // https://www.w3.org/TR/WCAG21/#contrast-minimum
  AALarge: 3,
  AAA: 7, // https://www.w3.org/TR/WCAG21/#contrast-enhanced
  AAALarge: 4.5,
};

const contrastingColor = memoize(({ color, level = 'AAA' }) => {
  const mixStep = 0.1;
  const mixColor = chroma(color).luminance() < 0.5 ? '#fff' : '#000';
  let mixture = 0;
  let outputColor = chroma.mix(color, mixColor, mixture);

  while (mixture <= 1) {
    const percent = mixture.toFixed(2);
    outputColor = chroma.mix(color, mixColor, percent);

    if (chroma.contrast(color, outputColor) >= contrastRatios[level]) {
      break;
    }

    mixture += mixStep;
  }

  return outputColor.css();
});

export default contrastingColor;
