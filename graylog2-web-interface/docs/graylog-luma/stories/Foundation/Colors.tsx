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
import { SECTIONS } from './colorPalette';
import type { Swatch } from './colorPalette';
import { dividerColor } from './lumaTokens';

// Palette data lives in ./colorPalette so the Tokens page can reuse it.

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

  return withBlack >= withWhite
    ? { ratio: withBlack, text: 'black' as const }
    : { ratio: withWhite, text: 'white' as const };
};

const formatRatio = (ratio: number) => (ratio >= 10 ? ratio.toFixed(1) : ratio.toFixed(2)).replace(/\.?0+$/, '') || '0';

// ─── Layout ────────────────────────────────────────────────────────────────

// Border and spacing above come from the global H2 rule in .storybook/DocsContainer.tsx.
const SectionTitle = styled.h2`
  margin-bottom: ${({ theme }) => theme.spacings.xs};
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
  padding: 1px ${({ theme }) => theme.spacings.xs};
  border: 1px solid ${({ theme }) => dividerColor(theme)};
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
  gap: ${({ theme }) => `${theme.spacings.sm} ${theme.spacings.xs}`};
  margin-top: ${({ theme }) => theme.spacings.sm};
`;

const SwatchTile = styled.div<{ $hex: string }>`
  position: relative;
  height: 44px;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => dividerColor(theme)};
  background-color: ${({ $hex }) => $hex};
`;

const ContrastBadge = styled.span<{ $text: 'black' | 'white' }>`
  position: absolute;
  top: ${({ theme }) => theme.spacings.xs};
  left: ${({ theme }) => theme.spacings.xs};
  padding: 1px ${({ theme }) => theme.spacings.xxs};
  border-radius: 999px;
  font-size: 9px;
  font-weight: 700;
  line-height: 1.4;
  background-color: ${({ $text }) => ($text === 'black' ? '#000000' : '#FFFFFF')};
  color: ${({ $text }) => ($text === 'black' ? '#FFFFFF' : '#000000')};
`;

const SwatchLabel = styled.div`
  margin-top: ${({ theme }) => theme.spacings.xs};
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
