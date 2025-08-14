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
import { useRef, useState, useCallback, useLayoutEffect } from 'react';
import type Plotly from 'plotly.js/lib/core';
import type { PlotMouseEvent } from 'plotly.js';

type Anchor = Element;
export type ClickPoint = PlotMouseEvent['points'][number];

const getBarElement = (graphDiv: HTMLElement, curveNumber: number, pointIndex: number): Element | null => {
  const traces = graphDiv.querySelectorAll('.barlayer .trace');
  const trace = traces[curveNumber] as HTMLElement | undefined;
  if (!trace) return null;
  const points = trace.querySelectorAll('.point');
  const point = points[pointIndex] as HTMLElement | undefined;
  if (!point) return null;

  return point.querySelector('rect') ?? point.querySelector('path');
};

// 1) Return the DOM element for a *point* in a scatter/line trace.
// Prefer the marker shape if present; otherwise fall back to the line path.
const getScatterPointElement = (graphDiv: HTMLElement, curveNumber: number, pointIndex: number): Element | null => {
  // trace index within scatterlayer
  const traces = graphDiv.querySelectorAll('.scatterlayer .trace');
  const trace = traces[curveNumber] as HTMLElement | undefined;
  if (!trace) return null;

  // point node (exists when markers are rendered)
  const point = trace.querySelectorAll('.points .point')[pointIndex] as HTMLElement | undefined;

  // prefer the concrete point shape if it exists (path or circle)
  if (point) {
    return (
      point.querySelector('path') ||
      point.querySelector('circle') ||
      point.querySelector('use') || // symbols-as-use
      null
    );
  }

  // markers not rendered (mode: "lines" only) -> fall back to the line path
  // (you'll anchor to the whole line; see note below about precise position)
  return trace.querySelector('.lines > path');
};

const elementGetters = {
  bar: getBarElement,
  scatter: getScatterPointElement,
};

const contains = (r: DOMRect, x: number, y: number) => x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;

/** Distance from (x,y) to rect (0 if inside) */
const distToRect = (r: DOMRect, x: number, y: number) => {
  let dx = 0;
  if (x < r.left) {
    dx = r.left - x;
  } else if (x > r.right) {
    dx = x - r.right;
  }

  let dy = 0;
  if (y < r.top) {
    dy = r.top - y;
  } else if (y > r.bottom) {
    dy = y - r.bottom;
  }

  return Math.hypot(dx, dy);
};

type Picked = {
  pt: ClickPoint;
  el: Element;
  rect: DOMRect;
} | null;

/** Pick the point whose bar rect contains the click; fallback to nearest rect */
const pickPointByGeometry = (
  e: any,
  graphDiv: HTMLElement,
  getEl: (graphDiv: HTMLElement, curveNumber: number, pointIndex: number) => Element | null,
): Picked => {
  const { clientX, clientY } = e.event as MouseEvent;
  const candidates: Picked[] = (e.points as ClickPoint[])
    .map((pt) => {
      const el = getEl(graphDiv, pt.curveNumber, pt.pointIndex);
      if (!el) return null;
      const rect = el.getBoundingClientRect();

      return { pt, el, rect };
    })
    .filter((c): c is Picked => c !== null);

  if (!candidates.length) return null;

  // 1) exact hit: rect that contains the click
  const hit = candidates.find((c) => contains(c!.rect, clientX, clientY));
  if (hit) return hit;

  // 2) fallback: closest rect to the click point
  return candidates.reduce((best, c) => {
    if (!best) return c;

    return distToRect(c.rect, clientX, clientY) < distToRect(best.rect, clientX, clientY) ? c : best;
  }, null as Picked);
};

const usePositionUpdate = (anchor: Anchor, setFromRect: (el: Element) => void) =>
  useLayoutEffect(() => {
    if (!anchor) return undefined;
    let prev = { left: -1, top: -1, w: -1, h: -1 };
    let raf = 0;

    const tick = () => {
      if (!anchor) return;
      const r = anchor.getBoundingClientRect();
      // update only if changed to avoid extra renders
      if (r.left !== prev.left || r.top !== prev.top || r.width !== prev.w || r.height !== prev.h) {
        prev = { left: r.left, top: r.top, w: r.width, h: r.height };
        // setPos({ left: r.left + r.width / 2, top: r.top });
        setFromRect(anchor);
      }
      // eslint-disable-next-line compat/compat
      raf = requestAnimationFrame(tick);
    };

    // kick it off
    tick();

    // eslint-disable-next-line consistent-return
    return () => cancelAnimationFrame(raf);
  }, [anchor, setFromRect]);

type Rel = { x: number; y: number }; // range [0, 1]
type Pos = { left: number; top: number };

const usePlotOnClickPopover = (chartType: string) => {
  const graphDivRef = useRef<Plotly.PlotlyHTMLElement | null>(null);
  const [anchor, setAnchor] = useState<Anchor>(null);
  const [pos, setPos] = useState<Pos | null>({ left: 0, top: 0 });
  const [rel, setRel] = useState<Rel>({ x: 0.5, y: 0 });
  const [clickPoint, setClickPoint] = useState<ClickPoint | null>(null);
  const setFromRect = useCallback(
    (el: Element) => {
      const r = el.getBoundingClientRect();
      // setPos({ left: r.left + r.width / 2, top: r.top });
      setPos({
        left: r.left + r.width * rel.x,
        top: r.top + r.height * rel.y,
      });
    },
    [rel.x, rel.y],
  );

  usePositionUpdate(anchor, setFromRect);

  const onChartClick = (e: any) => {
    const graphDiv = graphDivRef.current ?? (e.event?.target as HTMLElement)?.closest('.js-plotly-plot');
    const picked = graphDiv ? pickPointByGeometry(e, graphDiv as HTMLElement, elementGetters[chartType]) : null;

    if (!picked || !graphDiv) return;

    const { el, rect, pt } = picked;
    if (!el) return;
    const { clientX, clientY } = e.event as MouseEvent;
    const clamp01 = (v: number) => Math.max(0, Math.min(1, v));
    const relX = clamp01((clientX - rect.left) / (rect.width || 1));
    const relY = clamp01((clientY - rect.top) / (rect.height || 1));

    setRel({ x: relX, y: relY });
    setFromRect(el);
    setAnchor(el);
    setClickPoint(pt);
  };

  const isPopoverOpen = !!anchor && !!pos;

  const onPopoverChange = (isOpen: boolean) => {
    if (!isOpen) {
      setAnchor(null);
      setPos(null);
    }
  };

  const initializeGraphDivRef = (_: Plotly.PlotlyHTMLElement, gd: Plotly.PlotlyHTMLElement) => {
    graphDivRef.current = gd;
  };

  return {
    initializeGraphDivRef,
    onChartClick,
    onPopoverChange,
    isPopoverOpen,
    pos,
    clickPoint,
  };
};

export default usePlotOnClickPopover;
