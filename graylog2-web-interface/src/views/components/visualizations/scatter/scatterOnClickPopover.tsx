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
import type { PlotMouseEvent, PlotlyHTMLElement, PlotData } from 'plotly.js';
import minBy from 'lodash/minBy';
import uniqBy from 'lodash/uniqBy';

import type { Rel, ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type {
  Px,
  Anchor,
  PlotlyHTMLElementWithInternals,
} from 'views/components/visualizations/OnClickPopover/anchors';
import { clamp01, distToRect, pickNearestElementAnchor } from 'views/components/visualizations/OnClickPopover/anchors';
import dropdownPopover from 'views/components/visualizations/OnClickPopover/dropdownPopover';
import CartesianOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/CartesianOnClickPopoverDropdown';
import { CANDIDATE_PICK_RADIUS } from 'views/components/visualizations/Constants';

const getScatterMarkerElement = (graphDiv: HTMLElement, pt: ClickPoint): Element | null => {
  const {
    fullData: { uid },
    pointIndex,
  } = pt;

  const trace = graphDiv.querySelector(`.scatterlayer .trace.trace${uid}`);
  if (!trace) return null;

  return trace.querySelectorAll('.points .point')[pointIndex] ?? null;
};

/**
 * Project a point P onto the line AB and return the parametric "t"
 * - If t < 0 → closest before A
 * - If t > 1 → closest after B
 * - If 0 <= t <= 1 → closest lies within segment AB
 */
const projectT = (P: Px, A: Px, B: Px) => {
  // Vector AB
  const vx = B.x - A.x;
  const vy = B.y - A.y;

  // Vector AP
  const wx = P.x - A.x;
  const wy = P.y - A.y;

  // Length squared of AB (avoid division by 0)
  const len2 = vx * vx + vy * vy || 1;

  // Dot product projection formula
  return (wx * vx + wy * vy) / len2;
};

/**
 * Convert a data-space coordinate (xVal, yVal) to **page pixel coordinates**
 * so we can compare against mouse clicks.
 */
const dataToPagePx = (
  gd: PlotlyHTMLElementWithInternals,
  pt: ClickPoint,
  xVal: any,
  yVal: any,
): { x: number; y: number } => {
  const fl = gd._fullLayout;

  // Get the axis objects (may be stored differently depending on pt)
  const xa = (pt as any).xaxis?.l2p ? (pt as any).xaxis : fl[(pt as any).xaxis?._name ?? 'xaxis'];
  const ya = (pt as any).yaxis?.l2p ? (pt as any).yaxis : fl[(pt as any).yaxis?._name ?? 'yaxis'];

  if (!xa?.l2p || !xa?.d2l || !ya?.l2p || !ya?.d2l) return null;

  // Convert data value → linearized → pixel coordinate within plot
  const gx = xa.l2p(xa.d2l(xVal));
  const gy = ya.l2p(ya.d2l(yVal));
  if (!Number.isFinite(gx) || !Number.isFinite(gy)) return null;

  // Add graph container offsets (_size.l/t are margins inside the SVG)
  const rect = gd.getBoundingClientRect();

  return { x: rect.left + fl._size.l + gx, y: rect.top + fl._size.t + gy };
};

function pickNearestLinesElementForTrace(gd: PlotlyHTMLElement, pt: ClickPoint, click: Px): Element | null {
  const uid = pt.fullData?.uid;
  if (!uid) return null;

  const trace = gd.querySelector<HTMLElement>(`.scatterlayer .trace.trace${uid}`);
  if (!trace) return null;

  const linesGroups = Array.from(trace.querySelectorAll<SVGGElement>('.lines path.js-line'));
  if (!linesGroups.length) return null;

  const candidates = linesGroups.map((line) => {
    const r = line.getBoundingClientRect();
    const { x, y } = click;

    return { el: line, d: distToRect(r, { x, y }) };
  });

  const best: { el: SVGGElement; d: number } = minBy(candidates, 'd');

  return best?.el;
}

/**
 * Given a click and a scatter point, return candidate "nearest line segment" anchors.
 * Each candidate has:
 *  - el: the line path element (so popover can stick to it)
 *  - pt: the Plotly point datum
 *  - d: distance from click to the projected point on the segment
 *  - px, py: projected point coordinates in page pixels
 *  - valuePx, valuePy: value point coordinates in page pixels
 */
const getScatterLineElements = (gd: PlotlyHTMLElement, click: Px, pt: ClickPoint) => {
  // Get the full data for this trace (array of x/y values)
  const fd: PlotData = pt.data ?? (gd as PlotlyHTMLElementWithInternals)._fullData?.[pt.curveNumber];
  const xs = fd?.x ?? [];
  const ys = fd?.y ?? [];

  // Current index in the data array
  const i: number = pt.pointIndex ?? pt.pointNumber ?? 0;

  // Build candidate segments: one before (i-1 → i), one after (i → i+1)
  const segs = [i > 0 ? [i - 1, i] : null, i < xs.length - 1 ? [i, i + 1] : null].filter((seg) => !!seg);

  return segs.map(([i0, i1]) => {
    // Convert endpoints from data space → page pixels
    const A = dataToPagePx(gd, pt, xs[i0], ys[i0]);
    const B = dataToPagePx(gd, pt, xs[i1], ys[i1]);
    if (!A || !B) return null;

    // Project the click point onto the line segment A→B
    // projectT gives the "t" position on the infinite line
    // clamp01 ensures it's within the segment [0,1]
    const t = clamp01(projectT(click, A, B));
    const px = A.x + t * (B.x - A.x);
    const py = A.y + t * (B.y - A.y);

    // Distance from click to this projected point
    const d = Math.hypot(click.x - px, click.y - py);
    // Find the actual <path> element for the line
    const el = pickNearestLinesElementForTrace(gd, pt, click);
    // The actual data point's pixel coordinates
    const { x: valuePx, y: valuePy } = dataToPagePx(gd, pt, xs[i], ys[i]);

    return { el, pt, d, px, py, valuePx, valuePy };
  });
};

const makeScatterAnchor = (e: PlotMouseEvent, gd: PlotlyHTMLElement): Anchor | null => {
  const graphDiv = gd;
  const markerCandidates = e.points
    .map((pt: ClickPoint) => {
      const el = getScatterMarkerElement(graphDiv, pt);

      return el ? { pt, el, rect: el.getBoundingClientRect() } : null;
    })
    .filter((candidate) => !!candidate);

  const bestMarker = pickNearestElementAnchor(e, markerCandidates);
  if (bestMarker) return bestMarker;
  const { clientX, clientY } = e.event;
  const click: Px = { x: clientX, y: clientY };
  const lineCandidates = e.points
    .flatMap((pt: ClickPoint) => getScatterLineElements(graphDiv, click, pt))
    .filter((candidate) => !!candidate);
  const best = minBy(lineCandidates, 'd');
  // we need unique pt because in this case one pt can have several related lines
  const pointsInRadius = uniqBy(
    lineCandidates
      .filter(({ d }) => d < CANDIDATE_PICK_RADIUS)
      .sort((a, b) => a.d - b.d)
      .map(({ pt }) => pt),
    'pointIndex',
  );

  if (!best) return null;
  const { el, pt, valuePx, valuePy } = best;
  if (!el) return null;
  const rect = el.getBoundingClientRect();
  const rel: Rel = {
    x: clamp01((valuePx - rect.left) / Math.max(rect.width, 1)),
    y: clamp01((valuePy - rect.top) / Math.max(rect.height, 1)),
  };

  return { rel, el, pt, pointsInRadius };
};

const scatterOnClickPopover = {
  buildAnchor: makeScatterAnchor,
  renderPopover: dropdownPopover(CartesianOnClickPopoverDropdown),
};

export default scatterOnClickPopover;
