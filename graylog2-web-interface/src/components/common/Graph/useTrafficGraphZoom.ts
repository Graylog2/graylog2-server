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
import { useCallback, useMemo, useState } from 'react';

import AppConfig from 'util/AppConfig';

import type { Traffic } from './types';

type TrafficGraphZoom = {
  zoomedToData: boolean;
  uiRevision: number;
  canZoomOrReset: boolean;
  onZoomReset: () => void;
  onUserZoom: () => void;
  onUserZoomReset: () => void;
};

/**
 * Owns the zoom state of a traffic graph: `zoomedToData` fits the y-axis to the data
 * instead of the traffic limit, and bumping `uiRevision` makes plotly discard all user
 * interaction state (e.g. a dragged time range), restoring the initial view.
 *
 * `traffic` must be the series the graph actually plots (the daily sums, not the raw
 * sub-daily API buckets) — the zoom-to-data gate compares its values against the limit.
 *
 * A single reset trigger always returns a zoomed graph (drag-zoomed, data-zoomed, or
 * both) to its initial view; only a pristine graph zooms to the data. Wire `onUserZoom`
 * to the plot's zoom event so drag-zooms are tracked, and `onUserZoomReset` to the
 * plot's reset event (plotly restores the view itself on double-click) so the trigger
 * stays in sync. `canZoomOrReset` is false when triggering would do nothing. All state
 * resets automatically when the traffic data changes.
 */
const useTrafficGraphZoom = (traffic: Traffic | null | undefined, trafficLimit?: number): TrafficGraphZoom => {
  const isCloud = AppConfig.isCloud();
  const [zoomedToData, setZoomedToData] = useState(false);
  const [userZoomed, setUserZoomed] = useState(false);
  const [uiRevision, setUiRevision] = useState(1);
  const [prevTraffic, setPrevTraffic] = useState(traffic);

  const resetToInitialView = useCallback(() => {
    setZoomedToData(false);
    setUserZoomed(false);
    setUiRevision((revision) => revision + 1);
  }, []);

  if (prevTraffic !== traffic) {
    setPrevTraffic(traffic);
    resetToInitialView();
  }

  const maxPlottedValue = useMemo(() => (traffic ? Math.max(0, ...Object.values(traffic)) : 0), [traffic]);
  const canZoomToData = Boolean(traffic && trafficLimit && maxPlottedValue < trafficLimit && !isCloud);
  const canZoomOrReset = zoomedToData || userZoomed || canZoomToData;

  const onUserZoom = useCallback(() => setUserZoomed(true), []);
  const onUserZoomReset = useCallback(() => setUserZoomed(false), []);

  const onZoomReset = useCallback(() => {
    const isPristine = !zoomedToData && !userZoomed;

    if (isPristine) {
      // Nothing to reset — zoom to the data if a limit dwarfs it, otherwise do nothing.
      if (canZoomToData) {
        setZoomedToData(true);
      }

      return;
    }

    resetToInitialView();
  }, [canZoomToData, resetToInitialView, userZoomed, zoomedToData]);

  return { zoomedToData, uiRevision, canZoomOrReset, onZoomReset, onUserZoom, onUserZoomReset };
};

export default useTrafficGraphZoom;
