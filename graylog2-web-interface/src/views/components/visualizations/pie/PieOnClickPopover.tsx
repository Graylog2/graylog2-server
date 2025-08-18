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
import styled, { css } from 'styled-components';

import type { ClickPoint } from 'views/components/visualizations/hooks/usePlotOnClickPopover';
import Value from 'views/components/Value';
import Popover from 'components/common/Popover';

const atIndex = (v: string | string[] | undefined, i: number | undefined): T | undefined => {
  if (v == null) return undefined;
  if (Array.isArray(v)) {
    if (typeof i !== 'number') return undefined;

    return (v as T[])[i];
  }

  return v as T;
};
/** Best-effort “hover swatch” color for common traces (bar/scatter/pie). */
export function getHoverSwatchColor(pt: ClickPoint): string | undefined {
  const fd = pt.fullData;

  const idx = pt.pointIndex ?? pt.pointNumber;

  // BAR / SCATTER MARKERS
  const markerPoint = atIndex(fd?.marker?.color, idx) ?? atIndex(fd?.marker?.line?.color, idx);
  if (markerPoint) return markerPoint;

  // PIE-like (colors array)
  const pieColor = atIndex(fd?.marker?.colors, idx) ?? atIndex(fd?.marker?.line?.color, idx);
  if (pieColor) return pieColor;

  // SCATTER LINES (no markers)
  const linePoint = atIndex(fd?.line?.color, idx) ?? fd?.fillcolor;
  if (linePoint) return linePoint;

  // Fallback: single-value marker/line colors (already handled above if arrays)
  return (fd?.marker && (fd.marker.color as string)) || (fd?.line && (fd.line.color as string)) || undefined;
}

const ValueBox = styled.span<{ $bgColor: string }>(
  ({ theme, $bgColor }) => css`
    background-color: ${$bgColor};
    color: ${theme.utils.contrastingColor($bgColor)};
    padding: ${theme.spacings.xxs};
  `,
);

const Container = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.tiny};
  `,
);

const PieOnClickPopover = ({ clickPoint }: { clickPoint: ClickPoint }) => {
  if (!clickPoint) return null;

  const traceColor = getHoverSwatchColor(clickPoint);

  const { v: value, pointNumber, data } = clickPoint;
  const valueText = data?.text?.[pointNumber];

  return (
    <Popover.Dropdown title={String(clickPoint?.label)}>
      <Value
        field={data.name}
        value={value}
        render={() => (
          <Container>
            <ValueBox $bgColor={traceColor}>{`${String(valueText ?? value)}`}</ValueBox>
            <span>{clickPoint.data.name}</span>
          </Container>
        )}
      />
    </Popover.Dropdown>
  );
};

export default PieOnClickPopover;
