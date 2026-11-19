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

import { ColorSwatch, HexValue, StoryContainer } from './shared';
import { PALETTES } from './colorPalette';
import type { PaletteKey } from './colorPalette';

const SwatchGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: ${({ theme }) => `${theme.spacings.sm} ${theme.spacings.xs}`};
`;

const SwatchLabel = styled.div`
  margin-top: ${({ theme }) => theme.spacings.xs};
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
`;

export const PaletteDoc = ({ palette }: { palette: PaletteKey }) => (
  <StoryContainer>
    <SwatchGrid>
      {PALETTES[palette].swatches.map((swatch) => (
        <div key={swatch.label}>
          <ColorSwatch color={swatch.hex} width="100%" height="44px" />
          <SwatchLabel>{swatch.label}</SwatchLabel>
          <HexValue>{swatch.hex}</HexValue>
        </div>
      ))}
    </SwatchGrid>
  </StoryContainer>
);
