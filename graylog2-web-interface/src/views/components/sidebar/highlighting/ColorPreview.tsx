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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';
import { scales } from 'plotly.js/src/components/colorscale';

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

const plotlyScaleToChroma = (plotlyScale: Array<[domain: number, color: string]>) => {
  const domains = plotlyScale.map(([domain]) => domain);
  const colors = plotlyScale.map(([, color]) => color);

  return chroma.scale(colors).domain(domains);
};

const scaleForGradient = (gradient: string): chroma.Scale => {
  switch (gradient) {
    case 'Blackbody':
    case 'Bluered':
    case 'Cividis':
    case 'Earth':
    case 'Electric':
    case 'Hot':
    case 'Jet':
    case 'Picnic':
    case 'Portland':
    case 'Rainbow': return plotlyScaleToChroma(scales[gradient]);
    default: return chroma.scale(gradient);
  }
};

const colorsForGradient = (gradient: string, count = 5): Array<string> => scaleForGradient(gradient).colors(count);

export const GradientColorPreview = styled(ColorPreviewBase)(({ gradient }: { gradient: string }) => {
  const colors = colorsForGradient(gradient);

  return css`
      border: none;
      background: linear-gradient(0deg, ${colors.map((color, idx) => `${color} ${idx * (100 / colors.length)}%`).join(', ')});
    `;
});

type ColorPreviewProps = {
  color: HighlightingColor,
  onClick?: () => void,
};

const ColorPreview = React.forwardRef<HTMLDivElement, ColorPreviewProps>(({ color, onClick = () => {} }, ref) => {
  if (color.isStatic()) {
    return <StaticColorPreview ref={ref} onClick={onClick} color={(color as StaticColor).color} />;
  }

  if (color.isGradient()) {
    return <GradientColorPreview ref={ref} onClick={onClick} gradient={(color as GradientColor).gradient} />;
  }

  throw new Error(`Invalid highlighting color type: ${color}`);
});

ColorPreview.propTypes = {
  color: PropTypes.any.isRequired,
  onClick: PropTypes.func,
};

ColorPreview.defaultProps = {
  onClick: () => {},
};

export default ColorPreview;
