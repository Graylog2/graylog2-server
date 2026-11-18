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
  height: 44px;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => dividerColor(theme)};
  background-color: ${({ $hex }) => $hex};
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

const ColorSwatch = ({ swatch }: { swatch: Swatch }) => (
  <div>
    <SwatchTile $hex={swatch.hex} />
    <SwatchLabel>{swatch.label}</SwatchLabel>
    <SwatchHex>{swatch.hex}</SwatchHex>
  </div>
);

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
