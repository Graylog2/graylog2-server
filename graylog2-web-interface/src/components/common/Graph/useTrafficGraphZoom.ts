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
import { useState } from 'react';

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

const useTrafficGraphZoom = (traffic: Traffic | null | undefined, trafficLimit?: number): TrafficGraphZoom => {
  const isCloud = AppConfig.isCloud();
  const [zoomedToData, setZoomedToData] = useState(false);
  const [userZoomed, setUserZoomed] = useState(false);
  const [uiRevision, setUiRevision] = useState(1);
  const [prevTraffic, setPrevTraffic] = useState(traffic);

  const resetToInitialView = () => {
    setZoomedToData(false);
    setUserZoomed(false);
    setUiRevision((revision) => revision + 1);
  };

  if (prevTraffic !== traffic) {
    setPrevTraffic(traffic);
    resetToInitialView();
  }

  const plottedValues = traffic ? Object.values(traffic) : [];
  const maxPlottedValue = Math.max(0, ...plottedValues);
  const canZoomToData = Boolean(plottedValues.length > 0 && trafficLimit && maxPlottedValue < trafficLimit && !isCloud);
  const canZoomOrReset = zoomedToData || userZoomed || canZoomToData;

  const onUserZoom = () => setUserZoomed(true);
  const onUserZoomReset = () => setUserZoomed(false);

  const onZoomReset = () => {
    const isPristine = !zoomedToData && !userZoomed;

    if (isPristine) {
      if (canZoomToData) {
        setZoomedToData(true);
      }

      return;
    }

    resetToInitialView();
  };

  return { zoomedToData, uiRevision, canZoomOrReset, onZoomReset, onUserZoom, onUserZoomReset };
};

export default useTrafficGraphZoom;
