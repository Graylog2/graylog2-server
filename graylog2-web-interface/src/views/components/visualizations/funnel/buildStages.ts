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
import chroma from 'chroma-js';

import type { LeafPath } from 'views/components/visualizations/utils/extractLeafPaths';

import type { FunnelStage } from './types';

export const formatCount = (n: number): string => {
  if (n >= 1_000_000) return `${+(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${+(n / 1_000).toFixed(1)}k`;

  return String(n);
};

/** Generate N shades of a base color, from slightly darker to slightly lighter. */
export const subPartColors = (base: string, n: number): string[] => {
  if (n <= 1) return [base];

  return chroma
    .scale([chroma(base).darken(0.5), chroma(base).brighten(0.7)])
    .mode('lab')
    .colors(n);
};

/**
 * Groups leaf paths into funnel stages.
 *
 * When column pivots are present each unique combination of row-pivot keys
 * becomes one stage, and the column-pivot values become that stage's sub-parts.
 * When there are no column pivots every path is its own single-part stage.
 */
export const buildStages = (
  paths: LeafPath[],
  displayKeys: string[][],
  rowFieldCount: number,
): FunnelStage[] => {
  const hasColumnPivots = (displayKeys[0]?.length ?? 0) > rowFieldCount;

  if (!hasColumnPivots) {
    return paths
      .map((path, i) => ({ label: displayKeys[i].join(' / '), value: path.value, parts: [] }))
      .sort((a, b) => b.value - a.value);
  }

  // Preserve insertion order (pivot data comes back in a consistent order from
  // the server) so sub-parts appear in the same order every render.
  const stageMap = new Map<string, FunnelStage>();

  paths.forEach((path, i) => {
    const rowKeys = displayKeys[i].slice(0, rowFieldCount);
    const colKeys = displayKeys[i].slice(rowFieldCount);
    const stageKey = rowKeys.join('\0');

    if (!stageMap.has(stageKey)) {
      stageMap.set(stageKey, { label: rowKeys.join(' / '), value: 0, parts: [] });
    }

    const stage = stageMap.get(stageKey)!;
    stage.value += path.value;
    stage.parts.push({ label: colKeys.join(' / '), value: path.value });
  });

  return Array.from(stageMap.values()).sort((a, b) => b.value - a.value);
};
