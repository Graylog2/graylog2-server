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

export type Opacify = {
  (string, number): string,
};

function opacify(color: string, amount: number): string {
  /**
   * Increases the opacity of a color. Its range for the amount is between 0 to 1.
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {number} amount - any positive number
   */

  if (color === 'transparent') {
    return color;
  }

  const parsedAlpha = chroma(color).alpha();
  const newAlpha = (parsedAlpha * 100 + amount * 100) / 100;

  return chroma(color).alpha(newAlpha).css();
}

export default opacify;
