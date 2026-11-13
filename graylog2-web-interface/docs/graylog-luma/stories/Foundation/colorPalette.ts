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
// Values match the current Graylog theme (@graylog/sawmill THEME_BASE): Gray = gray, Cyan = info,
// Yellow = warning, Green = success, Red = danger, Blue = primary. Violet and the syntax colors are
// Luma spec colors that aren't in the theme yet. Edit the data below to update the page.

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
        swatches: scale([
          '#F6F7FC',
          '#E1E4ED',
          '#C1C7DC',
          '#6F768F',
          '#394261',
          '#252D47',
          '#232B43',
          '#1C2235',
          '#12182B',
          '#060C1F',
        ]),
      },
      {
        name: 'Blue',
        swatches: scale([
          '#D9EBFF',
          '#ADD0FF',
          '#7FB8FF',
          '#65A9FF',
          '#4396FF',
          '#1877EF',
          '#1064CD',
          '#0C50A5',
          '#083772',
          '#032551',
        ]),
      },
    ],
  },
  {
    title: 'Semantic Colors',
    palettes: [
      {
        name: 'Yellow',
        description: 'Warning: used to indicate moderate urgency.',
        swatches: scale([
          '#FFFBEB',
          '#FFF2C2',
          '#FFEDAD',
          '#FFE485',
          '#FFDC5C',
          '#FFCA0A',
          '#FFCA0A',
          '#CCA000',
          '#7A6000',
          '#292000',
        ]),
      },
      {
        name: 'Green',
        description: 'Success: used to indicate a positive state.',
        swatches: scale([
          '#E1FFF4',
          '#ACFFE0',
          '#69F9C3',
          '#62F3BC',
          '#45E5A8',
          '#2ECA8F',
          '#22B07A',
          '#1A9869',
          '#128057',
          '#094F35',
        ]),
      },
      {
        name: 'Red',
        description: 'Danger: used to indicate immediate attention or a destructive action.',
        swatches: scale([
          '#FFEDED',
          '#FFC7C7',
          '#FFACAB',
          '#FE8685',
          '#FE6E6D',
          '#FE4A49',
          '#E74342',
          '#B43534',
          '#8C2928',
          '#6B1F1F',
        ]),
      },
    ],
  },
  {
    title: 'Accent Colors',
    intro: 'Our accent colors are used in infographics and dashboard elements.',
    palettes: [
      {
        name: 'Violet',
        swatches: scale([
          '#F5F0FF',
          '#E8DDFF',
          '#D9C7FF',
          '#CEB7FF',
          '#BA9AFF',
          '#9A6BFE',
          '#8C61E7',
          '#6D4CB4',
          '#553B8C',
          '#412D6B',
        ]),
      },
      {
        name: 'Cyan',
        swatches: scale([
          '#EBFAFF',
          '#C2F0FF',
          '#85E0FF',
          '#47D1FF',
          '#1FC7FF',
          '#03C2FF',
          '#00A8E0',
          '#008AB8',
          '#006B8F',
          '#003D52',
        ]),
      },
    ],
  },
  {
    title: 'Common Colors',
    palettes: [
      {
        swatches: [
          { label: 'White', hex: '#FFFFFF' },
          { label: 'Black', hex: '#0D0A1F' },
        ],
      },
    ],
  },
  {
    title: 'Syntax Colors',
    intro: 'This is used for highlighting the syntax in code.',
    palettes: [
      {
        swatches: [
          { label: 'Purple', hex: '#9747FF' },
          { label: 'Blue', hex: '#437AED' },
          { label: 'Maroon', hex: '#CD6069' },
          { label: 'Orange', hex: '#FF6D12' },
          { label: 'Green', hex: '#53A053' },
          { label: 'Violet', hex: '#C838C6' },
          { label: 'Teal', hex: '#1098AD' },
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
