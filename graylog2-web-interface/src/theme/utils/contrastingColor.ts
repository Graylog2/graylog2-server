/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import chroma from 'chroma-js';

export type ContrastingColor = {
  (color: string, wcagLevel?: string): string,
};

/**
 * Accepts a color and [WCAG distinguishable level](https://www.w3.org/TR/WCAG21/#distinguishable), it then returns a properly contrasting color.
 *
 * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
 * @param {string} [wcagLevel="AAA"] - Based on the [contrast calculations recommended by W3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html). (available levels: "AA", "AALarge", "AAA", "AAALarge")
 *
 * @returns {string}
 *
 */

const contrastRatios: { [key: string]: number } = {
  AA: 4.5, // https://www.w3.org/TR/WCAG21/#contrast-minimum
  AALarge: 3,
  AAA: 7, // https://www.w3.org/TR/WCAG21/#contrast-enhanced
  AAALarge: 4.5,
};

const contrastingColor = (color: string, wcagLevel: string = 'AAA'): string => {
  const mixStep = 0.05;
  const mixColor = chroma(color).luminance() < 0.5 ? '#fff' : '#000';
  let mixture = 0;
  let outputColor = chroma.mix(color, mixColor, mixture).css();

  while (mixture <= 1) {
    outputColor = chroma.mix(color, mixColor, mixture).css();

    if (chroma.contrast(color, outputColor) >= contrastRatios[wcagLevel]) {
      break;
    }

    mixture += mixStep;
  }

  return outputColor;
};

export default contrastingColor;
