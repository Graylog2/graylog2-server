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
import * as React from 'react';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import HighlightingColor, { GradientColor, StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';

const ColorPreviewBase = styled.div`
  height: 2rem;
  width: 2rem;
  margin-right: 0.4rem;

  border-radius: 4px;
  border: 1px solid rgba(0, 126, 255, 0.24);
`;

const StaticColorPreview = styled(ColorPreviewBase)(({ color }) => css`
  background-color: ${color}
`);

export const GradientColorPreview = styled(ColorPreviewBase)(({ gradient }: { gradient: string }) => {
  try {
    const colors = chroma.scale(gradient).colors(5);

    return css`
    background: linear-gradient(0deg, ${colors.map((color, idx) => `${color} ${idx * (100 / colors.length)}%`).join(', ')});
  `;
  } catch (e) {
    console.log(`Error for ${gradient}: ${e}`);
  }
});

const ColorPreview = ({ color }: { color: HighlightingColor }) => {
  switch (color.type) {
    case 'static': return <StaticColorPreview color={(color as StaticColor).color} />;
    case 'gradient': return <GradientColorPreview gradient={(color as GradientColor).gradient} />;
    default: throw new Error(`Invalid highlighting color type: ${color.type}`);
  }
};

export default ColorPreview;
