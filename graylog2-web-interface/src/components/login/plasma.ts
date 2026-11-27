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
type RGB = [number, number, number];

const PALETTE_SIZE = 256;
const COLOR_COUNT = 64;
const BAND_SIZE = PALETTE_SIZE / COLOR_COUNT;

// Cyclic, so the palette rotation has no visible seam.
const PALETTE_STOPS: Array<RGB> = [
  [16, 19, 34],
  [10, 58, 92],
  [11, 143, 196],
  [10, 58, 92],
  [20, 26, 58],
  [35, 36, 110],
  [20, 26, 58],
];

export const createPalette = () => {
  const palette = new Uint8ClampedArray(PALETTE_SIZE * 3);
  const segments = PALETTE_STOPS.length;

  for (let i = 0; i < PALETTE_SIZE; i += 1) {
    const band = Math.floor(i / BAND_SIZE) * BAND_SIZE;
    const position = (band / PALETTE_SIZE) * segments;
    const index = Math.floor(position);
    const fraction = position - index;
    const from = PALETTE_STOPS[index];
    const to = PALETTE_STOPS[(index + 1) % segments];
    const smooth = fraction * fraction * (3 - 2 * fraction);

    for (let channel = 0; channel < 3; channel += 1) {
      palette[i * 3 + channel] = from[channel] + (to[channel] - from[channel]) * smooth;
    }
  }

  return palette;
};

export const plasmaValue = (x: number, y: number, t: number) => {
  const cx = x + 0.5 * Math.sin(t / 5);
  const cy = y + 0.5 * Math.cos(t / 3);

  return (
    Math.sin(x * 10 + t) +
    Math.sin((y * 10 + t) / 2) +
    Math.sin((x * 10 + y * 10 + t) / 2) +
    Math.sin(Math.sqrt(100 * (cx * cx + cy * cy) + 1) + t)
  );
};

export const renderPlasmaFrame = (imageData: ImageData, palette: Uint8ClampedArray, t: number) => {
  const { width, height, data } = imageData;
  const shift = Math.floor(t * 6);
  const unit = 1 / height;

  for (let row = 0; row < height; row += 1) {
    const y = row * unit - 0.5;

    for (let column = 0; column < width; column += 1) {
      const x = column * unit - 0.5;
      const value = plasmaValue(x, y, t * 0.3);
      const paletteIndex = (Math.floor(((value + 4) / 8) * PALETTE_SIZE) + shift) % PALETTE_SIZE;
      const offset = (row * width + column) * 4;

      data[offset] = palette[paletteIndex * 3];
      data[offset + 1] = palette[paletteIndex * 3 + 1];
      data[offset + 2] = palette[paletteIndex * 3 + 2];
      data[offset + 3] = 255;
    }
  }
};
