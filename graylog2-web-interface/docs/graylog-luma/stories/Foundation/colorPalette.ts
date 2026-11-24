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
import { colorVariants } from 'theme/sawmill/THEME_BASE';

export type Swatch = {
  label: string;
  hex: string;
};

type Palette = {
  name?: string;
  swatches: Swatch[];
};

const scale = (hexes: string[]): Swatch[] => hexes.map((hex, i) => ({ label: String(i), hex }));

export const PALETTES = {
  gray: { name: 'Gray', swatches: scale(colorVariants.gray) },
  blue: { name: 'Blue', swatches: scale(colorVariants.primary) },
  yellow: { name: 'Yellow', swatches: scale(colorVariants.warning) },
  green: { name: 'Green', swatches: scale(colorVariants.success) },
  red: { name: 'Red', swatches: scale(colorVariants.danger) },
  violet: { name: 'Violet', swatches: scale(colorVariants.violet) },
  cyan: { name: 'Cyan', swatches: scale(colorVariants.info) },
  common: {
    swatches: [
      { label: 'White', hex: '#FFFFFF' },
      { label: 'Black', hex: colorVariants.default[8] },
    ],
  },
} satisfies Record<string, Palette>;

export type PaletteKey = keyof typeof PALETTES;

// Flat lookup of every palette color by hex, e.g. '#232B43' -> 'Gray 6'.
export const PALETTE_NAMES: Record<string, string> = Object.fromEntries(
  Object.values(PALETTES).flatMap((palette: Palette) =>
    palette.swatches.map((swatch) => [
      swatch.hex.toUpperCase(),
      palette.name ? `${palette.name} ${swatch.label}` : swatch.label,
    ]),
  ),
);
