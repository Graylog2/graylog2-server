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

import type { Colors } from 'theme/colors';

export type ColorLevel = (colorHex: string, level?: number) => string;

const colorLevel = (colors: Colors) => (colorHex: string, level: number = 0): string => {
  /**
   * Recreating `color-level` from Bootstrap's SCSS functions
   * https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {number} level - any positive or negative number
   */
  const colorBase = level > 0 ? colors.global.textDefault : colors.global.textAlt;
  const absLevel = Math.abs(level) * 0.08; // TODO: make 8% a theme variable
  const upperLevel = absLevel > 1 ? 1 : absLevel;
  const mixLevel = absLevel < 0 ? 0 : upperLevel;

  return chroma.mix(colorBase, colorHex, mixLevel).css();
};

export default colorLevel;
