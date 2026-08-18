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
import type { FunnelStage } from './types';

export const LABEL_AREA = 72; // px reserved at top for stage name + count
export const NODE_RATIO = 0.55; // fraction of column width occupied by the stage rect
export const FILL_RATIO = 0.88; // largest stage fills this fraction of the available height
export const MIN_STAGE_PX = 4; // minimum stage rect height so tiny stages remain visible
export const MIN_LABEL_PX = 22; // minimum sub-part height to show its inline label

export type FunnelGeometry = {
  colW: number;
  nW: number;
  stageH: (i: number) => number;
  top: (i: number) => number;
  bot: (i: number) => number;
  nl: (i: number) => number;
  nr: (i: number) => number;
  bcx: (i: number) => number;
  transitionPath: (i: number) => string;
};

/**
 * Derives all layout helpers from the stage list and SVG dimensions.
 * Returns a bag of pure functions so the SVG render stays declarative.
 */
export const computeFunnelGeometry = (stages: FunnelStage[], width: number, height: number): FunnelGeometry => {
  const maxValue = stages[0].value;
  const colW = width / stages.length;
  const nW = colW * NODE_RATIO;
  const shapeH = height - LABEL_AREA;
  const centerY = LABEL_AREA + shapeH / 2;

  const stageH = (i: number) => Math.max((stages[i].value / maxValue) * shapeH * FILL_RATIO, MIN_STAGE_PX);
  const top = (i: number) => centerY - stageH(i) / 2;
  const bot = (i: number) => centerY + stageH(i) / 2;
  const nl = (i: number) => i * colW;
  const nr = (i: number) => i * colW + nW;
  const bcx = (i: number) => i * colW + nW + (colW - nW) / 2;

  // Overlap 1px into both adjacent rects so sub-pixel anti-aliasing gaps are hidden.
  const transitionPath = (i: number) => {
    const lx = nr(i) - 1;
    const rx = nl(i + 1) + 1;

    return (
      `M ${lx},${top(i)} ` +
      `C ${bcx(i)},${top(i)} ${bcx(i)},${top(i + 1)} ${rx},${top(i + 1)} ` +
      `L ${rx},${bot(i + 1)} ` +
      `C ${bcx(i)},${bot(i + 1)} ${bcx(i)},${bot(i)} ${lx},${bot(i)} Z`
    );
  };

  return { colW, nW, stageH, top, bot, nl, nr, bcx, transitionPath };
};
