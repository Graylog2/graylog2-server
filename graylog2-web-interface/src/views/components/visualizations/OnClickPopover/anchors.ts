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
import type { PlotMouseEvent, PlotlyHTMLElement } from 'plotly.js';
import minBy from 'lodash/minBy';
import type React from 'react';

import type { Rel, ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { CANDIDATE_PICK_RADIUS } from 'views/components/visualizations/Constants';

export type Px = { x: number; y: number };

export interface PlotlyHTMLElementWithInternals extends PlotlyHTMLElement {
  _fullData?: any[];
  _fullLayout?: any;
}

/** A popover anchor: the DOM element the popover sticks to plus the clicked point(s). */
export type ElementAnchor = { el: Element; rel: Rel; pt: ClickPoint; pointsInRadius?: Array<ClickPoint> };
export type Anchor = ElementAnchor;

/** Builds a popover anchor from a Plotly click event. Provided per visualization. */
export type BuildAnchor = (e: PlotMouseEvent, gd: PlotlyHTMLElement) => Anchor | null;

export type RenderPopoverProps = {
  anchor: Anchor;
  config: AggregationWidgetConfig;
  onPopoverClose: () => void;
};

/** Renders the popover content for an anchor. Provided per visualization. */
export type RenderPopover = (props: RenderPopoverProps) => React.ReactNode;

/** An element getter resolves the DOM element for a clicked point. */
export type ElementGetter = (graphDiv: HTMLElement, pt: ClickPoint, targetEl: Element) => Element | null;

export const clamp01 = (v: number) => Math.max(0, Math.min(1, v));

export const distToRect = (rect: DOMRect, { x, y }: Px) => {
  let dx = 0;
  if (x < rect.left) dx = rect.left - x;
  else if (x > rect.right) dx = x - rect.right;
  let dy = 0;
  if (y < rect.top) dy = rect.top - y;
  else if (y > rect.bottom) dy = y - rect.bottom;

  return Math.hypot(dx, dy);
};

/** Returns the clicked element itself as the anchor element (used by pie & heatmap). */
export const getTargetElement: ElementGetter = (_: HTMLElement, __: ClickPoint, targetEl: Element): Element | null =>
  targetEl;

export const pickNearestElementAnchor = (
  e: PlotMouseEvent,
  candidates: { pt: ClickPoint; el: Element; rect: DOMRect }[],
): ElementAnchor | null => {
  const { clientX, clientY } = e.event;
  if (!candidates.length) return null;
  const inside = candidates.find(
    ({ rect }) => clientX >= rect.left && clientX <= rect.right && clientY >= rect.top && clientY <= rect.bottom,
  );

  const candidatesWithDistances = candidates.map((candidate) => {
    const rect = candidate.rect;
    const d = distToRect(rect, { x: clientX, y: clientY });

    return { d, candidate };
  });

  const pointsInRadius = candidatesWithDistances
    .filter(({ d }) => d < CANDIDATE_PICK_RADIUS)
    .sort((a, b) => a.d - b.d)
    .map(({ candidate }) => candidate.pt);

  const closest = inside ?? minBy(candidatesWithDistances, 'd').candidate;
  if (!closest) return null;
  const { el, rect, pt } = closest;
  const rel: Rel = {
    x: clamp01((clientX - rect.left) / Math.max(rect.width, 1)),
    y: clamp01((clientY - rect.top) / Math.max(rect.height, 1)),
  };

  return { el, rel, pt, pointsInRadius };
};

/**
 * Builds an anchor by resolving a DOM element for each clicked point (via `getEl`)
 * and picking the one nearest the click. Used by element-based charts (bar, pie, heatmap).
 */
export const makeElementAnchor = (
  e: PlotMouseEvent,
  gd: PlotlyHTMLElement,
  getEl: ElementGetter,
): ElementAnchor | null => {
  const graphDiv = gd;
  const targetEl = (e.event?.target as Element) ?? graphDiv;
  const candidates = e.points
    .map((pt: ClickPoint) => {
      const el = getEl(graphDiv, pt, targetEl);

      return el ? { pt, el, rect: el.getBoundingClientRect() } : null;
    })
    .filter((candidate) => !!candidate);

  return pickNearestElementAnchor(e, candidates);
};
