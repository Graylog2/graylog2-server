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
import { clamp01 } from 'views/components/visualizations/OnClickPopover/anchors';
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
  const pt = anchor.pt as SankeyClickPoint & {
    curveNumber?: number;
    customdata?: { source?: unknown; target?: unknown };
  };
  // A link is either plotly's native sankey link (`source`/`target` at the top level) or a
  // customdata-encoded edge (e.g. network graph edges rendered as scatter traces).
  const isLink =
    isSankeyLinkPoint(anchor.pt) ||
    !!(pt.customdata && typeof pt.customdata === 'object' && pt.customdata.source && pt.customdata.target);
  const curve = pt.curveNumber ?? 0;
  const idx = pt.pointNumber ?? pt.index ?? 0;

  return `${curve}-${isLink ? 'l' : 'n'}-${idx}`;
};

const makeSankeyAnchor = (e: PlotMouseEvent, gd: PlotlyHTMLElement): Anchor | null => {
  const graphDiv = gd;
  const pt = e.points?.[0] as ClickPoint | undefined;

  if (!pt) return null;

  // Sankey dispatches clicks via Plotly's `Fx.click(gd, { target: true })`, so the event handed to
  // us (`e.event`) is a synthetic `{ target: true }` with no coordinates. The real DOM event — with
  // the clicked SVG element and page coordinates — is stashed on the point as `originalEvent`.
  const domEvent = (pt as { originalEvent?: MouseEvent }).originalEvent;

  // Prefer the element the user actually clicked: its bounding box always contains the click,
  // and it sidesteps the fragile assumption that a link's data index matches the DOM order of
  // `.sankey-link` paths. Fall back to the index lookup, then to the whole plot.
  const targetEl = (domEvent?.target as Element | null) ?? null;
  const el = targetEl?.closest?.('.sankey-link, .sankey-node') ?? getSankeyAnchorElement(graphDiv, pt) ?? graphDiv;

  // Anchor at the actual click position within the element. Sankey links are wide curved bands,
  // so their bounding-box center is nowhere near the click — anchoring there put the popover in
  // the wrong place. Deriving `rel` from the click matches how the other visualizations anchor.
  const rect = el.getBoundingClientRect();
  const rel = domEvent
    ? {
        x: clamp01((domEvent.clientX - rect.left) / Math.max(rect.width, 1)),
        y: clamp01((domEvent.clientY - rect.top) / Math.max(rect.height, 1)),
      }
    : { x: 0.5, y: 0.5 };

  return { el, pt, rel };
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
