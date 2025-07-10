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
import { useCallback, useEffect, useRef, useState } from 'react';
import { CircleMarker, MapContainer, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import chroma from 'chroma-js';
import flatten from 'lodash/flatten';
import leafletStyles from 'leaflet/dist/leaflet.css';

import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';

import style from './MapVisualization.css';

import InteractiveContext from '../../contexts/InteractiveContext';

const DEFAULT_VIEWPORT = Viewport.create([0, 0], 1);

type MapVisualizationProps = {
  attribution?: string;
  onRenderComplete?: () => void;
  markerRadiusSize?: number;
  markerRadiusIncrementSize?: number;
  id: string;
  url?: string;
  locked?: boolean;
  viewport?: Viewport;
  height: number;
  width: number;
  onChange: (newViewport: Viewport) => void;
  data: Array<{ keys: any; name: any; values: { [key: string]: number } }>;
};

const _getBucket = (value: number, bucketCount: number, minValue: number, maxValue: number, increment: number) => {
  // Calculate bucket size based on min/max value and the number of buckets.
  const bucketSize = (maxValue - minValue) / bucketCount;
  // Select bucket for the given value.
  const bucket = value < maxValue ? Math.ceil((value - minValue) / bucketSize) : bucketCount;

  return bucket + increment;
};

// Coordinates are given as "lat,long"
type MarkerProps = {
  coordinates: string;
  value: number;
  min: number;
  max: number;
  radiusSize: number;
  increment: number;
  color: chroma.Color;
  name: JSX.Element;
  keys: { [s: string]: unknown } | ArrayLike<unknown>;
};

const Marker = ({ coordinates, value, min, max, radiusSize, increment, color, name, keys }: MarkerProps) => {
  const formattedCoordinates = coordinates
    .split(',')
    .map((component) => Number(component))
    // eslint-disable-next-line no-restricted-globals
    .filter((n) => !isNaN(n));

  if (formattedCoordinates.length !== 2) {
    return null;
  }

  const radius = _getBucket(value, radiusSize, min, max, increment);
  const markerKeys = flatten(
    Object.entries(keys).map(([k, v]) => [
      <dt key={`dt-${k}-${v}`}>{k}</dt>,
      <dd key={`dd-${k}-${v}`}>{v as React.ReactNode}</dd>,
    ]),
  );

  return (
    <CircleMarker
      key={`${name}-${coordinates}`}
      center={formattedCoordinates as [number, number]}
      radius={radius}
      color={color.hex()}
      fillColor={color.hex()}
      weight={2}
      opacity={0.8}>
      <Popup>
        <dl>
          <dt>Name</dt>
          <dd>{name}</dd>
          {markerKeys}
          <dt>Coordinates:</dt>
          <dd>{coordinates}</dd>
          {value && (
            <>
              <dt>Value:</dt>
              <dd>{value}</dd>
            </>
          )}
        </dl>
      </Popup>
    </CircleMarker>
  );
};

const MapEvents = ({
  onViewportChanged,
}: {
  onViewportChanged: React.ComponentProps<typeof MapVisualization>['onChange'];
}) => {
  const map = useMap();

  const _onViewportChanged = () => {
    const { lat, lng } = map.getCenter();

    return onViewportChanged(
      Viewport.create([Number.parseFloat(lat.toFixed(4)), Number.parseFloat(lng.toFixed(4))], map.getZoom()),
    );
  };

  useMapEvents({
    dragend: _onViewportChanged,
    zoomend: _onViewportChanged,
  });

  return null;
};

type MapRef = React.ComponentProps<typeof MapContainer>['ref'];

const defaultOnRenderComplete = () => {};
const MapVisualization = ({
  attribution = '&copy; <a href="http://osm.org/copyright" target="_blank">OpenStreetMap</a> contributors',
  data,
  height,
  id,
  locked = false,
  markerRadiusIncrementSize = 10,
  markerRadiusSize = 10,
  onChange,
  onRenderComplete = defaultOnRenderComplete,
  url = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
  viewport = DEFAULT_VIEWPORT,
  width,
}: MapVisualizationProps) => {
  const [_isMapReady, setIsMapReady] = useState<boolean>(false);
  const [_areTilesReady, setAreTilesReady] = useState<boolean>(false);
  const [_viewport, setViewport] = useState<Viewport>(DEFAULT_VIEWPORT);
  const _map: MapRef = useRef();

  useEffect(() => {
    leafletStyles.use();

    return leafletStyles.unuse;
  }, []);

  useEffect(() => {
    if (_viewport && _map?.current) {
      if (viewport.center !== _viewport.center || viewport.zoom !== _viewport.zoom) {
        _map.current.setView([viewport.center[0], viewport.center[1]], viewport.zoom);
      }
    }
    // Leaving out _viewport from dependencies, so it only runs when prop has changed.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewport.center, viewport.zoom]);

  const _handleRenderComplete = useCallback(() => {
    if (_areTilesReady && _isMapReady) {
      onRenderComplete();
    }
  }, [_areTilesReady, _isMapReady, onRenderComplete]);

  const _handleMapReady = useCallback(() => {
    setIsMapReady(true);
    _handleRenderComplete();
  }, [_handleRenderComplete]);

  const _handleTilesReady = useCallback(() => {
    setAreTilesReady(true);
    _handleRenderComplete();
  }, [_handleRenderComplete]);

  const _onChange = useCallback(
    (newViewport: Viewport) => {
      setViewport(newViewport);

      return onChange(newViewport);
    },
    [onChange],
  );

  const noOfKeys = data.length;
  const chromaScale = chroma.scale('Spectral');

  const markers = data.flatMap(({ keys, name, values }, idx) => {
    const y = Object.values(values);
    const min = Math.min(...y);
    const max = Math.max(...y);
    const color = chromaScale(idx * (1 / noOfKeys));

    return Object.entries(values).map(([coord, value], valueIdx) => (
      <Marker
        key={`${name}-${coord}-${value}`}
        coordinates={coord}
        value={value}
        min={min}
        max={max}
        radiusSize={markerRadiusSize}
        increment={markerRadiusIncrementSize}
        color={color}
        name={name}
        keys={keys[valueIdx]}
      />
    ));
  });

  return (
    <InteractiveContext.Consumer>
      {(interactive) => (
        <div className={locked ? style.mapLocked : ''} style={{ position: 'relative', zIndex: 0 }}>
          {locked && <div className={style.overlay} style={{ height, width }} />}
          <MapContainer
            ref={_map}
            boundsOptions={{ maxZoom: 19, animate: interactive }}
            center={viewport.center}
            className={style.map}
            closePopupOnClick={interactive}
            doubleClickZoom={interactive}
            dragging={interactive}
            fadeAnimation={interactive}
            id={`visualization-${id}`}
            key={`visualization-${id}-${width}-${height}`}
            markerZoomAnimation={interactive}
            scrollWheelZoom={interactive}
            style={{ height, width }}
            touchZoom={interactive}
            trackResize={interactive}
            whenReady={_handleMapReady}
            zoom={viewport.zoom}
            zoomAnimation={interactive}
            zoomControl={interactive}>
            <MapEvents onViewportChanged={_onChange} />
            <TileLayer url={url} attribution={attribution} eventHandlers={{ load: _handleTilesReady }} />
            {markers}
          </MapContainer>
        </div>
      )}
    </InteractiveContext.Consumer>
  );
};

export default MapVisualization;
