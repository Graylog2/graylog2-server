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
// ─── Luma color palette ────────────────────────────────────────────────────
// Scales come from the theme's color variants (src/theme/sawmill/THEME_BASE.ts).
import { colorVariants } from 'theme/sawmill/THEME_BASE';

export type Swatch = {
  label: string;
  hex: string;
};

export type Palette = {
  name?: string;
  description?: string;
  swatches: Swatch[];
};

export type Section = {
  title: string;
  intro?: string;
  palettes: Palette[];
};

const scale = (hexes: string[]): Swatch[] => hexes.map((hex, i) => ({ label: String(i), hex }));

export const SECTIONS: Section[] = [
  {
    title: 'Core Brand Colors',
    intro: 'Our primary palette is used for our core UI elements and content.',
    palettes: [
      {
        name: 'Gray',
        swatches: scale(colorVariants.gray),
      },
      {
        name: 'Blue',
        swatches: scale(colorVariants.primary),
      },
    ],
  },
  {
    title: 'Semantic Colors',
    palettes: [
      {
        name: 'Yellow',
        description: 'Warning: used to indicate moderate urgency.',
        swatches: scale(colorVariants.warning),
      },
      {
        name: 'Green',
        description: 'Success: used to indicate a positive state.',
        swatches: scale(colorVariants.success),
      },
      {
        name: 'Red',
        description: 'Danger: used to indicate immediate attention or a destructive action.',
        swatches: scale(colorVariants.danger),
      },
    ],
  },
  {
    title: 'Accent Colors',
    intro: 'Our accent colors are used in infographics and dashboard elements.',
    palettes: [
      {
        name: 'Violet',
        swatches: scale(colorVariants.violet),
      },
      {
        name: 'Cyan',
        swatches: scale(colorVariants.info),
      },
    ],
  },
  {
    title: 'Common Colors',
    palettes: [
      {
        swatches: [
          { label: 'White', hex: '#FFFFFF' },
          { label: 'Black', hex: colorVariants.default[8] },
        ],
      },
    ],
  },
];

// Flat lookup of every palette color by hex, e.g. '#232B43' -> 'Gray 6'.
export const PALETTE_NAMES: Record<string, string> = Object.fromEntries(
  SECTIONS.flatMap((section) =>
    section.palettes.flatMap((palette) =>
      palette.swatches.map((swatch) => [
        swatch.hex.toUpperCase(),
        palette.name ? `${palette.name} ${swatch.label}` : swatch.label,
      ]),
    ),
  ),
);

// Hex value of a palette color, e.g. paletteHex('Gray', '2'). Unnamed palettes (Common) use their label: paletteHex(undefined, 'White').
export const paletteHex = (paletteName: string | undefined, label: string) =>
  SECTIONS.flatMap((section) => section.palettes)
    .filter((palette) => palette.name === paletteName)
    .flatMap((palette) => palette.swatches)
    .find((swatch) => swatch.label === label)?.hex;
