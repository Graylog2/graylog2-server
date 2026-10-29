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
import React from 'react';
import styled from 'styled-components';

import { StoryContainer } from './shared';

// ─── Luma color palette ────────────────────────────────────────────────────
// Source: Luma "Colors" design spec. Edit the data below to update the page.

type Swatch = {
  label: string;
  hex: string;
};

type Palette = {
  name?: string;
  description?: string;
  swatches: Swatch[];
};

type Section = {
  title: string;
  intro?: string;
  palettes: Palette[];
};

const scale = (hexes: string[]): Swatch[] => hexes.map((hex, i) => ({ label: String(i), hex }));

const SECTIONS: Section[] = [
  {
    title: 'Core Brand Colors',
    intro: 'Our primary palette is used for our core UI elements and content.',
    palettes: [
      {
        name: 'Gray',
        swatches: scale(['#F6F7FC', '#E5E9F3', '#C1C7DC', '#6F768F', '#394261', '#252D47', '#232B43', '#1C2235', '#12182B', '#060C1F']),
      },
      {
        name: 'Cyan',
        swatches: scale(['#EBFAFF', '#C2F0FF', '#85E0FF', '#47D1FF', '#1FC7FF', '#03C2FF', '#00A8E0', '#008AB8', '#006B8F', '#003D52']),
      },
    ],
  },
  {
    title: 'Semantic Colors',
    palettes: [
      {
        name: 'Yellow',
        description: 'Warning: used to indicate moderate urgency.',
        swatches: scale(['#FFF6E6', '#FFE4B0', '#FFD78A', '#FFC554', '#FFB933', '#FFA800', '#E89900', '#B57700', '#8C5C00', '#6B4700']),
      },
      {
        name: 'Green',
        description: 'Success: used to indicate a positive state.',
        swatches: scale(['#E1FFF4', '#ACFFE0', '#69F9C3', '#62F3BC', '#45E5A8', '#2ECA8F', '#22B07A', '#1A9869', '#128057', '#094F35']),
      },
      {
        name: 'Red',
        description: 'Danger: used to indicate immediate attention or a destructive action.',
        swatches: scale(['#FFEDED', '#FFC7C7', '#FFACAB', '#FE8685', '#FE6E6D', '#FE4A49', '#E74342', '#B43534', '#8C2928', '#6B1F1F']),
      },
    ],
  },
  {
    title: 'Accent Colors',
    intro: 'Our accent colors are used in infographics and dashboard elements.',
    palettes: [
      {
        name: 'Violet',
        swatches: scale(['#F5F0FF', '#E8DDFF', '#D9C7FF', '#CEB7FF', '#BA9AFF', '#9A6BFE', '#8C61E7', '#6D4CB4', '#553B8C', '#412D6B']),
      },
      {
        name: 'Blue',
        swatches: scale(['#D9EBFF', '#ADD0FF', '#7FB8FF', '#65A9FF', '#4396FF', '#1877EF', '#1064CD', '#0C50A5', '#083772', '#032551']),
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

// ─── Contrast (WCAG 2.x) ───────────────────────────────────────────────────

const luminance = (hex: string) => {
  const n = parseInt(hex.slice(1), 16);
  const [r, g, b] = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => {
    const c = v / 255;

    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });

  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

// Picks black or white text, whichever contrasts more, and returns that ratio.
const bestContrast = (hex: string) => {
  const l = luminance(hex);
  const withBlack = (l + 0.05) / 0.05;
  const withWhite = 1.05 / (l + 0.05);

  return withBlack >= withWhite ? { ratio: withBlack, text: 'black' as const } : { ratio: withWhite, text: 'white' as const };
};

const formatRatio = (ratio: number) => (ratio >= 10 ? ratio.toFixed(1) : ratio.toFixed(2)).replace(/\.?0+$/, '') || '0';

// ─── Layout ────────────────────────────────────────────────────────────────

const SectionTitle = styled.h2`
  margin: ${({ theme }) => `${theme.spacings.xl} 0 ${theme.spacings.xs}`};
  padding-bottom: ${({ theme }) => theme.spacings.xs};
  border-bottom: 1px solid rgba(128, 128, 128, 0.2);
`;

const PaletteBlock = styled.div`
  margin: ${({ theme }) => `${theme.spacings.lg} 0`};
`;

const PaletteHeader = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xs};
`;

const PaletteName = styled.h3`
  margin: 0;
  font-weight: 700;
`;

const CountPill = styled.span`
  padding: 1px 8px;
  border: 1px solid rgba(128, 128, 128, 0.35);
  border-radius: 999px;
  font-size: 11px;
  color: ${({ theme }) => theme.colors.text.secondary};
`;

const Muted = styled.p`
  max-width: 560px;
  margin: ${({ theme }) => `${theme.spacings.xxs} 0 ${theme.spacings.sm}`};
  color: ${({ theme }) => theme.colors.text.secondary};
`;

const SwatchGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: ${({ theme }) => `${theme.spacings.sm} 6px`};
  margin-top: ${({ theme }) => theme.spacings.sm};
`;

const SwatchTile = styled.div<{ $hex: string }>`
  position: relative;
  height: 44px;
  border-radius: 4px;
  border: 1px solid rgba(128, 128, 128, 0.25);
  background-color: ${({ $hex }) => $hex};
`;

const ContrastBadge = styled.span<{ $text: 'black' | 'white' }>`
  position: absolute;
  top: 5px;
  left: 5px;
  padding: 1px 5px;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 700;
  line-height: 1.4;
  background-color: ${({ $text }) => ($text === 'black' ? '#000000' : '#FFFFFF')};
  color: ${({ $text }) => ($text === 'black' ? '#FFFFFF' : '#000000')};
`;

const SwatchLabel = styled.div`
  margin-top: 6px;
  font-size: 12px;
  font-weight: 700;
`;

const SwatchHex = styled.div`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: 11px;
  color: ${({ theme }) => theme.colors.text.secondary};
`;

const ColorSwatch = ({ swatch }: { swatch: Swatch }) => {
  const { ratio, text } = bestContrast(swatch.hex);

  return (
    <div>
      <SwatchTile $hex={swatch.hex}>
        <ContrastBadge $text={text} title={`Contrast ${ratio.toFixed(2)}:1 with ${text} text`}>
          {formatRatio(ratio)}
        </ContrastBadge>
      </SwatchTile>
      <SwatchLabel>{swatch.label}</SwatchLabel>
      <SwatchHex>{swatch.hex}</SwatchHex>
    </div>
  );
};

const Count = ({ n }: { n: number }) => <CountPill>{n} colors</CountPill>;

export const ColorsDoc = () => (
  <StoryContainer>
    {SECTIONS.map((section) => {
      const single = section.palettes.length === 1 && !section.palettes[0].name;

      return (
        <section key={section.title}>
          <SectionTitle>{section.title}</SectionTitle>
          {section.intro && <Muted>{section.intro}</Muted>}
          {section.palettes.map((palette) => (
            <PaletteBlock key={`${section.title}-${palette.name ?? ''}`}>
              {(palette.name || single) && (
                <PaletteHeader>
                  {palette.name && <PaletteName>{palette.name}</PaletteName>}
                  <Count n={palette.swatches.length} />
                </PaletteHeader>
              )}
              {palette.description && <Muted>{palette.description}</Muted>}
              <SwatchGrid>
                {palette.swatches.map((swatch) => (
                  <ColorSwatch key={`${swatch.label}-${swatch.hex}`} swatch={swatch} />
                ))}
              </SwatchGrid>
            </PaletteBlock>
          ))}
        </section>
      );
    })}
  </StoryContainer>
);
