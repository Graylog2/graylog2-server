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
import type * as React from 'react';
import { useState, useRef, useCallback, useEffect } from 'react';

import useViewsDispatch from 'views/stores/useViewsDispatch';
import OnZoom from 'views/components/visualizations/OnZoom';
import useUserDateTime from 'hooks/useUserDateTime';

const MIN_BRUSH_PX = 5;
const ZOOM_FACTOR = 0.2;
const WHEEL_DEBOUNCE_MS = 300;

type Options = {
  dataMinTs: number;
  dataMaxTs: number;
  plotW: number;
  labelWidth: number;
  svgRef: React.RefObject<SVGSVGElement | null>;
};

export type BrushRect = { x: number; width: number };

type SwimlaneTimeNav = {
  viewMin: number;
  viewMax: number;
  xOf: (ts: number) => number;
  brushRect: BrushRect | null;
  isBrushing: boolean;
  isViewNarrowed: boolean;
  mouseHandlers: {
    onMouseDown: (e: React.MouseEvent) => void;
    onMouseMove: (e: React.MouseEvent) => void;
    onMouseUp: (e: React.MouseEvent) => void;
    onMouseLeave: () => void;
  };
};

const useSwimlaneTimeNav = ({ dataMinTs, dataMaxTs, plotW, labelWidth, svgRef }: Options): SwimlaneTimeNav => {
  const dispatch = useViewsDispatch();
  const { userTimezone } = useUserDateTime();

  const [viewMin, setViewMin] = useState(dataMinTs);
  const [viewMax, setViewMax] = useState(dataMaxTs);
  const [brushStart, setBrushStart] = useState<number | null>(null);
  const [brushCurrent, setBrushCurrent] = useState<number | null>(null);

  // Always-current view values for use inside event callbacks
  const viewRef = useRef({ min: viewMin, max: viewMax });
  viewRef.current = { min: viewMin, max: viewMax };

  const wheelTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Stable wheel handler via ref — lets us attach a non-passive listener once
  const wheelHandlerRef = useRef<((e: WheelEvent) => void) | null>(null);

  // Sync view to data range when backend delivers new results (but not during a brush)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (brushStart === null) {
      setViewMin(dataMinTs);
      setViewMax(dataMaxTs);
    }
  }, [dataMinTs, dataMaxTs]);

  // Attach non-passive wheel listener so we can prevent page scroll while zooming
  useEffect(() => {
    const svgEl = svgRef.current;
    if (!svgEl) return undefined;
    const listener = (e: WheelEvent) => wheelHandlerRef.current?.(e);
    svgEl.addEventListener('wheel', listener, { passive: false });

    return () => svgEl.removeEventListener('wheel', listener);
  }, [svgRef]);

  const getSvgX = useCallback(
    (clientX: number) => clientX - (svgRef.current?.getBoundingClientRect().left ?? 0),
    [svgRef],
  );

  const tsOfPx = useCallback(
    (svgX: number) => viewRef.current.min + ((svgX - labelWidth) / plotW) * (viewRef.current.max - viewRef.current.min),
    [labelWidth, plotW],
  );

  const commitZoom = useCallback(
    (fromMs: number, toMs: number) => {
      const lo = Math.min(fromMs, toMs);
      const hi = Math.max(fromMs, toMs);
      if (hi - lo < 1000) return;
      dispatch(OnZoom(new Date(lo).toISOString(), new Date(hi).toISOString(), userTimezone));
    },
    [dispatch, userTimezone],
  );

  // Update wheel handler ref every render so it always reads latest state/callbacks
  wheelHandlerRef.current = (e: WheelEvent) => {
    e.preventDefault();
    const { min, max } = viewRef.current;
    const svgX = e.clientX - (svgRef.current?.getBoundingClientRect().left ?? 0);
    const pivotTs = min + ((svgX - labelWidth) / plotW) * (max - min);
    const range = max - min || 1;
    const factor = e.deltaY > 0 ? 1 + ZOOM_FACTOR : 1 - ZOOM_FACTOR;
    const newRange = range * factor;
    const ratio = (pivotTs - min) / range;
    const newMin = pivotTs - ratio * newRange;
    const newMax = pivotTs + (1 - ratio) * newRange;

    viewRef.current = { min: newMin, max: newMax };
    setViewMin(newMin);
    setViewMax(newMax);

    if (wheelTimerRef.current) clearTimeout(wheelTimerRef.current);
    wheelTimerRef.current = setTimeout(() => commitZoom(newMin, newMax), WHEEL_DEBOUNCE_MS);
  };

  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      const svgX = getSvgX(e.clientX);
      if (svgX < labelWidth) return;
      e.preventDefault();
      setBrushStart(svgX);
      setBrushCurrent(svgX);
    },
    [getSvgX, labelWidth],
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      if (brushStart === null) return;
      setBrushCurrent(getSvgX(e.clientX));
    },
    [brushStart, getSvgX],
  );

  const handleMouseUp = useCallback(
    (e: React.MouseEvent) => {
      if (brushStart === null) return;
      const svgX = getSvgX(e.clientX);
      const dragWidth = Math.abs(svgX - brushStart);
      if (dragWidth >= MIN_BRUSH_PX) {
        commitZoom(tsOfPx(brushStart), tsOfPx(svgX));
      }
      setBrushStart(null);
      setBrushCurrent(null);
    },
    [brushStart, getSvgX, commitZoom, tsOfPx],
  );

  const handleMouseLeave = useCallback(() => {
    setBrushStart(null);
    setBrushCurrent(null);
  }, []);

  const xOf = useCallback(
    (ts: number) => labelWidth + ((ts - viewMin) / (viewMax - viewMin || 1)) * plotW,
    [viewMin, viewMax, labelWidth, plotW],
  );

  const isBrushing = brushStart !== null && brushCurrent !== null && Math.abs(brushCurrent - brushStart) >= MIN_BRUSH_PX;

  const brushRect: BrushRect | null = isBrushing
    ? { x: Math.min(brushStart!, brushCurrent!), width: Math.abs(brushCurrent! - brushStart!) }
    : null;

  const isViewNarrowed = viewMin > dataMinTs + 1000 || viewMax < dataMaxTs - 1000;

  return {
    viewMin,
    viewMax,
    xOf,
    brushRect,
    isBrushing,
    isViewNarrowed,
    mouseHandlers: {
      onMouseDown: handleMouseDown,
      onMouseMove: handleMouseMove,
      onMouseUp: handleMouseUp,
      onMouseLeave: handleMouseLeave,
    },
  };
};

export default useSwimlaneTimeNav;
