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
import type { PlotMouseEvent, PlotlyHTMLElement } from 'plotly.js';

import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type { Anchor, ElementAnchor, RenderPopover } from 'views/components/visualizations/OnClickPopover/anchors';
import SankeyOnClickPopover from 'views/components/visualizations/OnClickPopover/SankeyOnClickPopover';

type SankeyClickPoint = ClickPoint & { source?: Array<unknown>; target?: Array<unknown>; index?: number };

const isSankeyLinkPoint = (pt: SankeyClickPoint): boolean => !Array.isArray(pt?.source) && !Array.isArray(pt?.target);

const getSankeyAnchorElement = (gd: HTMLElement, pt: SankeyClickPoint): Element | null => {
  const idx = pt.index ?? pt.pointNumber;

  if (typeof idx !== 'number') return null;

  const selector = isSankeyLinkPoint(pt) ? '.sankey-link' : '.sankey-node';
  const elements = gd.querySelectorAll(selector);

  return elements[idx] ?? null;
};

const sankeyAnchorKey = (anchor: ElementAnchor): string => {
  const pt: SankeyClickPoint = anchor.pt;
  const isLink = isSankeyLinkPoint(anchor.pt);
  const idx = pt.pointNumber ?? pt.index ?? 0;

  return `${isLink ? 'l' : 'n'}-${idx}`;
};

const makeSankeyAnchor = (e: PlotMouseEvent, gd: PlotlyHTMLElement): Anchor | null => {
  const graphDiv = gd;
  const pt = e.points?.[0] as ClickPoint | undefined;

  if (!pt) return null;

  const el = getSankeyAnchorElement(graphDiv, pt) ?? graphDiv;

  return { el, pt, rel: { x: 0.5, y: 0.5 } };
};

const renderPopover: RenderPopover = ({ anchor, config, onPopoverClose }) => (
  <SankeyOnClickPopover
    // Remount per anchor so internal selection state resets when the user clicks a different element.
    key={sankeyAnchorKey(anchor)}
    clickPoint={anchor.pt}
    config={config}
    onPopoverClose={onPopoverClose}
  />
);

const sankeyOnClickPopover = {
  buildAnchor: makeSankeyAnchor,
  renderPopover,
};

export default sankeyOnClickPopover;
