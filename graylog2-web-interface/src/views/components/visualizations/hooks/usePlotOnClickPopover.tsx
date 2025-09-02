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
import { useRef, useState, useCallback } from 'react';
import type { PlotMouseEvent, PlotlyHTMLElement, PlotData } from 'plotly.js';
import map from 'lodash/map';
import compact from 'lodash/compact';
import flatMap from 'lodash/flatMap';
import minBy from 'lodash/minBy';
import { useFloating } from '@floating-ui/react';

import type { Rel, ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type { OnClickMarkerEvent } from 'views/components/visualizations/GenericPlot';
import OnClickPopoverWrapper from 'views/components/visualizations/OnClickPopover/OnClickPopoverWrapper';
import CartesianOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/CartesianOnClickPopoverDropdown';
import HeatmapOnClickPopover from 'views/components/visualizations/heatmap/HeatmapOnClickPopover';
import PieOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/PieOnClickPopoverDropdown';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

const clamp01 = (v: number) => Math.max(0, Math.min(1, v));

/** ---------- DOM getters ---------- */
const getBarElement = (graphDiv: HTMLElement, pt: ClickPoint): Element | null => {
  const { curveNumber, pointIndex } = pt;
  const trace = graphDiv.querySelectorAll('.barlayer .trace')[curveNumber] as HTMLElement | undefined;
  if (!trace) return null;
  const point = trace.querySelectorAll('.point')[pointIndex!] as HTMLElement | undefined;

  return point?.querySelector('rect') ?? point?.querySelector('path') ?? null;
};

const getScatterMarkerElement = (graphDiv: HTMLElement, pt: ClickPoint): Element | null => {
  const {
    fullData: { uid },
    pointIndex,
  } = pt;

  const trace = graphDiv.querySelector(`.scatterlayer .trace.trace${uid}`) as HTMLElement | undefined;
  if (!trace) return null;

  return trace.querySelectorAll('.points .point')[pointIndex] ?? null;
};

const getPieSliceElement = (_: HTMLElement, __: ClickPoint, targetEl: Element): Element | null => targetEl;

/** ---------- math helpers ---------- */
type Px = { x: number; y: number };
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
const dataToPagePx = (gd: PlotlyHTMLElementWithInternals, pt: ClickPoint, xVal: any, yVal: any) => {
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
  const rect = (gd as HTMLElement).getBoundingClientRect();

  return { x: rect.left + fl._size.l + gx, y: rect.top + fl._size.t + gy };
};

/** ---------- Anchors ---------- */
type ElementAnchor = { kind: 'element'; el: Element; rel: Rel; pt: ClickPoint };
type Anchor = ElementAnchor;

/** ---------- nearest element anchor ---------- */
const pickNearestElementAnchor = (
  e: PlotMouseEvent,
  candidates: { pt: ClickPoint; el: Element; rect: DOMRect }[],
): ElementAnchor | null => {
  const { clientX, clientY } = e.event as MouseEvent;
  if (!candidates.length) return null;
  const inside = candidates.find(
    ({ rect }) => clientX >= rect.left && clientX <= rect.right && clientY >= rect.top && clientY <= rect.bottom,
  );

  // TODO: add extra candidates to if distance is not so big
  /*
  const candidatesWithDistances = candidates.map((candidate) => {
    const rect = candidate.rect as DOMRect;
    let dx = 0;
    if (clientX < rect.left) dx = rect.left - clientX;
    else if (clientX > rect.right) dx = clientX - rect.right;
    let dy = 0;
    if (clientY < rect.top) dy = rect.top - clientY;
    else if (clientY > rect.bottom) dy = clientY - rect.bottom;
    const d = Math.hypot(dx, dy);

    return { d, candidate };
  });
  */

  const picked =
    inside ??
    minBy(candidates, ({ rect }) => {
      let dx = 0;
      if (clientX < rect.left) dx = rect.left - clientX;
      else if (clientX > rect.right) dx = clientX - rect.right;
      let dy = 0;
      if (clientY < rect.top) dy = rect.top - clientY;
      else if (clientY > rect.bottom) dy = clientY - rect.bottom;

      return Math.hypot(dx, dy);
    });
  if (!picked) return null;
  const { el, rect, pt } = picked;
  const rel: Rel = {
    x: clamp01((clientX - rect.left) / Math.max(rect.width, 1)),
    y: clamp01((clientY - rect.top) / Math.max(rect.height, 1)),
  };

  return { kind: 'element', el, rel, pt };
};

/** ---------- bar/pie anchors ---------- */
const makeElementAnchor = (
  e: PlotMouseEvent,
  gd: PlotlyHTMLElement,
  chartType: 'bar' | 'pie' | 'heatmap',
): ElementAnchor | null => {
  const getEl = chartType === 'bar' ? getBarElement : getPieSliceElement;
  const graphDiv = gd;
  const targetEl = (e.event?.target as Element) || graphDiv;
  const candidates = compact(
    map(e.points as ClickPoint[], (pt) => {
      const el = getEl(graphDiv, pt, targetEl);

      return el ? { pt, el, rect: el.getBoundingClientRect() } : null;
    }),
  );

  return pickNearestElementAnchor(e, candidates);
};

/**
 * Given a click and a scatter point, return candidate "nearest line segment" anchors.
 * Each candidate has:
 *  - el: the line path element (so popover can stick to it)
 *  - pt: the Plotly point datum
 *  - d: distance from click to the projected point on the segment
 *  - px, py: projected point coordinates in page pixels
 *  - valuePx, valuePy: value point coordinates in page pixels
 */

interface PlotlyHTMLElementWithInternals extends PlotlyHTMLElement {
  _fullData?: any[];
  _fullLayout?: any;
}

const getScatterLineElements = (gd: PlotlyHTMLElement, click: Px, pt: ClickPoint) => {
  // Get the full data for this trace (array of x/y values)
  const fd: PlotData = pt.data ?? (gd as PlotlyHTMLElementWithInternals)._fullData?.[pt.curveNumber];
  const xs = fd?.x ?? [];
  const ys = fd?.y ?? [];

  // Current index in the data array
  const i = (pt.pointIndex ?? pt.pointNumber ?? 0) as number;

  // Build candidate segments: one before (i-1 → i), one after (i → i+1)
  const segs: [number, number][] = compact([i > 0 ? [i - 1, i] : null, i < xs.length - 1 ? [i, i + 1] : null]);

  return map(segs, ([i0, i1]) => {
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
    const el = (gd.querySelectorAll('.scatterlayer .trace')[pt.curveNumber] as HTMLElement)?.querySelector(
      '.lines path.js-line',
    ) as Element | null;

    // The actual data point's pixel coordinates
    const { x: valuePx, y: valuePy } = dataToPagePx(gd, pt, xs[i], ys[i]);

    return { el, pt, d, px, py, valuePx, valuePy };
  });
};

const makeScatterAnchor = (e: PlotMouseEvent, gd: PlotlyHTMLElement): Anchor | null => {
  const graphDiv = gd;
  const markerCandidates = compact(
    map(e.points, (pt: ClickPoint) => {
      const el = getScatterMarkerElement(graphDiv, pt as ClickPoint);

      return el ? { pt, el, rect: el.getBoundingClientRect() } : null;
    }),
  );
  const bestMarker = pickNearestElementAnchor(e, markerCandidates);
  if (bestMarker) return bestMarker;
  const { clientX, clientY } = e.event as MouseEvent;
  const click: Px = { x: clientX, y: clientY };
  const lineCandidates = compact(
    flatMap(e.points as ClickPoint[], (pt) => getScatterLineElements(graphDiv, click, pt)),
  );
  const best = minBy(lineCandidates, 'd');
  if (!best) return null;
  const { el, pt, valuePx, valuePy } = best;
  if (!el) return null;
  const rect = el.getBoundingClientRect();
  const rel: Rel = {
    x: clamp01((valuePx - rect.left) / Math.max(rect.width, 1)),
    y: clamp01((valuePy - rect.top) / Math.max(rect.height, 1)),
  };

  return { kind: 'element', rel, el, pt };
};

type ChartType = 'bar' | 'scatter' | 'pie' | 'heatmap';

const popoverComponent = (chartType: ChartType) => {
  switch (chartType) {
    case 'heatmap':
      return HeatmapOnClickPopover;
    case 'pie':
      return PieOnClickPopoverDropdown;
    default:
      return CartesianOnClickPopoverDropdown;
  }
};
/** ---------- hook ---------- */
const usePlotOnClickPopover = (chartType: ChartType, config: AggregationWidgetConfig) => {
  const gdRef = useRef<PlotlyHTMLElement | null>(null);
  const [anchor, setAnchor] = useState<Anchor | null>(null);
  const [clickPoint, setClickPoint] = useState<ClickPoint | null>(null);
  const { refs, floatingStyles } = useFloating({
    elements: {
      reference: anchor?.el,
    },
    transform: false,
  });

  const initializeGraphDivRef = (_: unknown, gd: PlotlyHTMLElement) => {
    gdRef.current = gd;
  };

  const applyAnchor = useCallback((a: ElementAnchor) => {
    setAnchor(a);
    setClickPoint(a.pt);
  }, []);

  const onChartClick = (_: OnClickMarkerEvent, e: PlotMouseEvent) => {
    const gd =
      gdRef.current ?? ((e.event?.target as HTMLElement)?.closest('.js-plotly-plot') as PlotlyHTMLElement | null);
    if (!gd) return;
    const a = chartType === 'scatter' ? makeScatterAnchor(e, gd) : makeElementAnchor(e, gd, chartType);
    if (!a) return;
    applyAnchor(a);
  };

  const onPopoverChange = (isOpen: boolean) => {
    if (!isOpen) setAnchor(null);
  };

  const isPopoverOpen = !!anchor;

  const PopoverComponent = popoverComponent(chartType);

  const popover = (
    <OnClickPopoverWrapper
      isPopoverOpen={isPopoverOpen}
      onPopoverChange={onPopoverChange}
      ref={refs.setFloating}
      style={floatingStyles}>
      <PopoverComponent clickPoint={clickPoint} config={config} />
    </OnClickPopoverWrapper>
  );

  return { initializeGraphDivRef, onChartClick, popover };
};

export default usePlotOnClickPopover;
