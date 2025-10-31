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

import type { Color } from 'plotly.js';

import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';

const atIndex = (v: Color | Array<Color>, i: number): string | number => {
  if (Array.isArray(v)) {
    if (typeof i !== 'number') return undefined;

    return v[i] as string | number;
  }

  return v;
};

const getHoverSwatchColor = (pt: ClickPoint): string | number | undefined => {
  if (!pt) return null;
  const fd = pt.data;

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
  if (!Array.isArray(fd?.marker?.color) && !Array(fd?.line?.color)) {
    return fd?.marker?.color ?? (fd?.line?.color as string | number);
  }

  return undefined;
};

export default getHoverSwatchColor;
