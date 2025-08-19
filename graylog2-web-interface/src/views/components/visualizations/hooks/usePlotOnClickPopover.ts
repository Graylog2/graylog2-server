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
import { useRef, useState, useLayoutEffect } from 'react';
import type Plotly from 'plotly.js/lib/core';
import type { PlotDatum } from 'plotly.js/lib/core';
import type { PlotMouseEvent, PlotlyHTMLElement } from 'plotly.js';
import map from 'lodash/map';
import compact from 'lodash/compact';
import flatMap from 'lodash/flatMap';
import minBy from 'lodash/minBy';

type ClickPoint = PlotMouseEvent['points'][number];
type Pos = { left: number; top: number } | null;
type Rel = { x: number; y: number };

const clamp01 = (v: number) => Math.max(0, Math.min(1, v));

/** ---------- DOM getters ---------- */
const getBarElement = (graphDiv: HTMLElement, pt: PlotDatum): Element | null => {
  const { curveNumber, pointIndex } = pt;
  const trace = graphDiv.querySelectorAll('.barlayer .trace')[curveNumber] as HTMLElement | undefined;
  if (!trace) return null;
  const point = trace.querySelectorAll('.point')[pointIndex!] as HTMLElement | undefined;

  return point?.querySelector('rect') ?? point?.querySelector('path') ?? null;
};

const getScatterMarkerElement = (graphDiv: HTMLElement, pt: PlotDatum): Element | null => {
  const { curveNumber, pointIndex } = pt;
  const trace = graphDiv.querySelectorAll('.scatterlayer .trace')[curveNumber] as HTMLElement | undefined;

  if (!trace) return null;

  return trace.querySelectorAll('.points .point')[pointIndex!] ?? null;
};

const getPieSliceElement = (_: HTMLElement, __: PlotDatum, targetEl: Element): Element | null => targetEl;

/** ---------- math helpers ---------- */
type Px = { x: number; y: number };

const projectT = (P: Px, A: Px, B: Px) => {
  const vx = B.x - A.x;
  const vy = B.y - A.y;
  const wx = P.x - A.x;
  const wy = P.y - A.y;
  const len2 = vx * vx + vy * vy || 1;

  return (wx * vx + wy * vy) / len2;
};

const dataToPagePx = (gd: any, pt: ClickPoint, xVal: any, yVal: any) => {
  const fl = gd._fullLayout;
  const xa = (pt as any).xaxis?.l2p ? (pt as any).xaxis : fl[(pt as any).xaxis?._name ?? 'xaxis'];
  const ya = (pt as any).yaxis?.l2p ? (pt as any).yaxis : fl[(pt as any).yaxis?._name ?? 'yaxis'];
  if (!xa?.l2p || !xa?.d2l || !ya?.l2p || !ya?.d2l) return null;
  const gx = xa.l2p(xa.d2l(xVal));
  const gy = ya.l2p(ya.d2l(yVal));
  if (!Number.isFinite(gx) || !Number.isFinite(gy)) return null;
  const rect = (gd as HTMLElement).getBoundingClientRect();

  return { x: rect.left + fl._size.l + gx, y: rect.top + fl._size.t + gy };
};

/** ---------- Anchors ---------- */
type ElementAnchor = { kind: 'element'; el: Element; rel: Rel; pt: ClickPoint };
type Anchor = ElementAnchor;

/** ---------- rAF updater ---------- */
const useAnchorPosition = (anchor: Anchor | null, gdRef: React.RefObject<PlotlyHTMLElement>) => {
  const [pos, setPos] = useState<Pos>(null);

  useLayoutEffect(() => {
    if (!anchor) return;
    let rafId = 0 as unknown as number;
    let prev = { left: NaN, top: NaN };

    const tick = () => {
      let p: Pos = null;
      if (anchor.kind === 'element') {
        const r = anchor.el.getBoundingClientRect();
        p = { left: r.left + r.width * anchor.rel.x, top: r.top + r.height * anchor.rel.y };
      }

      if (p && (p.left !== prev.left || p.top !== prev.top)) {
        prev = p;
        setPos(p);
      }
      // eslint-disable-next-line compat/compat
      rafId = requestAnimationFrame(tick);
    };
    tick();

    // eslint-disable-next-line consistent-return
    return () => cancelAnimationFrame(rafId);
  }, [anchor, gdRef]);

  return pos;
};

/** ---------- shared nearest element picker ---------- */
const pickNearestElementAnchor = (
  e: PlotMouseEvent,
  candidates: { pt: ClickPoint; el: Element; rect: DOMRect }[],
): ElementAnchor | null => {
  const { clientX, clientY } = e.event as MouseEvent;
  if (!candidates.length) return null;

  const inside = candidates.find(
    ({ rect }) => clientX >= rect.left && clientX <= rect.right && clientY >= rect.top && clientY <= rect.bottom,
  );

  const picked =
    inside ??
    minBy(candidates, ({ rect }) => {
      let dx = 0;
      if (clientX < rect.left) {
        dx = rect.left - clientX;
      } else if (clientX > rect.right) {
        dx = clientX - rect.right;
      }

      let dy = 0;
      if (clientY < rect.top) {
        dy = rect.top - clientY;
      } else if (clientY > rect.bottom) {
        dy = clientY - rect.bottom;
      }

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

/** ---------- make element anchor (bar / pie) ---------- */
const makeElementAnchor = (
  e: PlotMouseEvent,
  gd: PlotlyHTMLElement,
  chartType: 'bar' | 'pie',
): ElementAnchor | null => {
  const getEl =
    chartType === 'bar'
      ? getBarElement
      : (graphDiv: HTMLElement, pt: PlotDatum, targetEl: Element) => getPieSliceElement(graphDiv, pt, targetEl);

  const graphDiv = gd as unknown as HTMLElement;
  const targetEl = (e.event?.target as Element) || graphDiv;

  const candidates = compact(
    map(e.points as ClickPoint[], (pt) => {
      const el = getEl(graphDiv, pt, targetEl);

      return el ? { pt, el, rect: el.getBoundingClientRect() } : null;
    }),
  );

  return pickNearestElementAnchor(e, candidates);
};

const getScatterLineElements = (gd: HTMLElement, click: Px, pt: PlotDatum) => {
  const fd: any = pt.fullData ?? (gd as any)._fullData?.[pt.curveNumber];
  const xs: any[] = fd?.x ?? [];
  const ys: any[] = fd?.y ?? [];
  const i = (pt.pointIndex ?? pt.pointNumber ?? 0) as number;
  const segs: [number, number][] = compact([i > 0 ? [i - 1, i] : null, i < xs.length - 1 ? [i, i + 1] : null]);

  return map(segs, ([i0, i1]) => {
    const A = dataToPagePx(gd, pt, xs[i0], ys[i0]);
    const B = dataToPagePx(gd, pt, xs[i1], ys[i1]);
    if (!A || !B) return null;
    const t = clamp01(projectT(click, A, B));
    const px = A.x + t * (B.x - A.x);
    const py = A.y + t * (B.y - A.y);
    const d = Math.hypot(click.x - px, click.y - py);

    const el = (gd.querySelectorAll('.scatterlayer .trace')[pt.curveNumber] as HTMLElement)?.querySelector(
      '.lines path.js-line',
    ) as Element | null;

    return { el, pt, d, px, py };
  });
};
/** ---------- scatter anchor ---------- */
const makeScatterAnchor = (e: PlotMouseEvent, gd: PlotlyHTMLElement): Anchor | null => {
  const graphDiv = gd as unknown as HTMLElement;

  // try markers
  const markerCandidates = compact(
    map(e.points, (pt) => {
      const el = getScatterMarkerElement(graphDiv, pt as PlotDatum);

      return el ? { pt, el, rect: el.getBoundingClientRect() } : null;
    }),
  );

  const bestMarker = pickNearestElementAnchor(e, markerCandidates);
  if (bestMarker) return bestMarker;

  // otherwise lines
  const { clientX, clientY } = e.event as MouseEvent;
  const click: Px = { x: clientX, y: clientY };

  const lineCandidates = compact(
    flatMap(e.points as ClickPoint[], (pt) => getScatterLineElements(graphDiv, click, pt)),
  );

  const { el, pt, px, py } = minBy(lineCandidates, 'd');
  const rect = el.getBoundingClientRect();

  const rel: Rel = {
    x: clamp01((px - rect.left) / Math.max(rect.width, 1)),
    y: clamp01((py - rect.top) / Math.max(rect.height, 1)),
  };

  return { kind: 'element', rel, el, pt };
};

/** ---------- hook ---------- */
const usePlotOnClickPopover = (chartType: 'bar' | 'scatter' | 'pie') => {
  const gdRef = useRef<Plotly.PlotlyHTMLElement | null>(null);
  const [anchor, setAnchor] = useState<Anchor | null>(null);
  const [clickPoint, setClickPoint] = useState<ClickPoint | null>(null);

  const pos = useAnchorPosition(anchor, gdRef);

  const initializeGraphDivRef = (_: Plotly.PlotlyHTMLElement, gd: Plotly.PlotlyHTMLElement) => {
    gdRef.current = gd;
  };

  const onChartClick = (e: PlotMouseEvent) => {
    const gd =
      gdRef.current ?? ((e.event?.target as HTMLElement)?.closest('.js-plotly-plot') as PlotlyHTMLElement | null);
    if (!gd) return;

    if (chartType === 'scatter') {
      const a = makeScatterAnchor(e, gd);
      if (!a) return;
      setAnchor(a);
      setClickPoint(a.pt);

      return;
    }

    // bar / pie
    const a = makeElementAnchor(e, gd, chartType);
    if (!a) return;
    setAnchor(a);
    setClickPoint(a.pt);
  };

  const onPopoverChange = (isOpen: boolean) => {
    if (!isOpen) setAnchor(null);
  };

  const isPopoverOpen = !!anchor && !!pos;

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
