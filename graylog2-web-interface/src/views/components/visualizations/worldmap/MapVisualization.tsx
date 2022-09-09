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
/* eslint-env browser */
import PropTypes from 'prop-types';
import React from 'react';
import { CircleMarker, MapContainer, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import chroma from 'chroma-js';
import { flatten } from 'lodash';
import leafletStyles from 'leaflet/dist/leaflet.css';

import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';

import style from './MapVisualization.css';

import InteractiveContext from '../../contexts/InteractiveContext';

const DEFAULT_VIEWPORT = {
  center: [0, 0],
  zoom: 1,
};

type MapVisualizationProps = {
  attribution?: string,
  onRenderComplete?: () => void,
  markerRadiusSize?: number,
  markerRadiusIncrementSize?: number,
  id: string,
  url?: string,
  locked?: boolean,
  viewport?: Viewport,
  height: number,
  width: number,
  onChange: (newViewport: Viewport) => void,
  data: Array<{ keys: any, name: any, values: { [key: string]: number } }>
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
  coordinates: string,
  value: number,
  min: number,
  max: number,
  radiusSize: number,
  increment: number,
  color: chroma.Color,
  name: JSX.Element,
  keys: { [s: string]: unknown; } | ArrayLike<unknown>,
};

const Marker = ({ coordinates, value, min, max, radiusSize, increment, color, name, keys }: MarkerProps) => {
  // eslint-disable-next-line no-restricted-globals
  const formattedCoordinates = coordinates.split(',').map((component) => Number(component)).filter((n) => !isNaN(n));

  if (formattedCoordinates.length !== 2) {
    return null;
  }

  const radius = _getBucket(value, radiusSize, min, max, increment);
  const markerKeys = flatten(Object.entries(keys).map(([k, v]) => [<dt key={`dt-${k}-${v}`}>{k}</dt>, <dd key={`dd-${k}-${v}`}>{v}</dd>]));

  return (
    <CircleMarker key={`${name}-${coordinates}`}
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
          {value
            && (
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

const MapEvents = ({ onViewportChanged }: { onViewportChanged: React.ComponentProps<typeof MapVisualization>['onChange'] }) => {
  const map = useMap();

  const _onViewportChanged = () => {
    const { lat, lng } = map.getCenter();

    return onViewportChanged(Viewport.create([lat, lng], map.getZoom()));
  };

  useMapEvents({
    dragend: _onViewportChanged,
    zoomend: _onViewportChanged,
  });

  return null;
};

class MapVisualization extends React.Component<MapVisualizationProps> {
  _isMapReady = false;

  _areTilesReady = false;

  static propTypes = {
    id: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(PropTypes.object),
    height: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
    url: PropTypes.string,
    attribution: PropTypes.string,
    onRenderComplete: PropTypes.func,
    onChange: PropTypes.func.isRequired,
    locked: PropTypes.bool, // Disables zoom and dragging
    markerRadiusSize: PropTypes.number,
    markerRadiusIncrementSize: PropTypes.number,
    viewport: PropTypes.shape({
      center: PropTypes.arrayOf(PropTypes.number),
      zoom: PropTypes.number,
    }),
  };

  static defaultProps = {
    data: {},
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="http://osm.org/copyright" target="_blank">OpenStreetMap</a> contributors',
    onRenderComplete: () => {},
    locked: false,
    viewport: DEFAULT_VIEWPORT,
    markerRadiusSize: 10,
    markerRadiusIncrementSize: 10,
  };

  componentDidMount() {
    leafletStyles.use();
  }

  componentWillUnmount() {
    leafletStyles.unuse();
  }

  _handleRenderComplete = () => {
    if (this._areTilesReady && this._isMapReady) {
      const { onRenderComplete } = this.props;

      onRenderComplete();
    }
  };

  _handleMapReady = () => {
    this._isMapReady = true;
    this._handleRenderComplete();
  };

  _handleTilesReady = () => {
    this._areTilesReady = true;
    this._handleRenderComplete();
  };

  render() {
    const { data, id, height, width, url, attribution, locked, viewport, onChange, markerRadiusSize, markerRadiusIncrementSize } = this.props;

    const noOfKeys = data.length;
    const chromaScale = chroma.scale('Spectral');

    const markers = data.flatMap(({ keys, name, values }, idx) => {
      const y = Object.values(values);
      const min = Math.min(...y);
      const max = Math.max(...y);
      const color = chromaScale(idx * (1 / noOfKeys));

      return Object.entries(values)
        .map(([coord, value], valueIdx) => (
          // eslint-disable-next-line react/no-array-index-key
          <Marker key={`${name}-${coord}-${value}`}
                  coordinates={coord}
                  value={value}
                  min={min}
                  max={max}
                  radiusSize={markerRadiusSize}
                  increment={markerRadiusIncrementSize}
                  color={color}
                  name={name}
                  keys={keys[valueIdx]} />
        ));
    });

    return (
      <InteractiveContext.Consumer>
        {(interactive) => (
          <div className={locked ? style.mapLocked : ''} style={{ position: 'relative', zIndex: 0 }}>
            {locked && <div className={style.overlay} style={{ height, width }} />}
            <MapContainer center={viewport.center}
                          boundsOptions={{ maxZoom: 19, animate: interactive }}
                          zoom={viewport.zoom}
                          className={style.map}
                          fadeAnimation={interactive}
                          key={`visualization-${id}-${width}-${height}`}
                          id={`visualization-${id}`}
                          markerZoomAnimation={interactive}
                          scrollWheelZoom
                          style={{ height, width }}
                          whenReady={this._handleMapReady}
                          zoomAnimation={interactive}>
              <MapEvents onViewportChanged={onChange} />
              <TileLayer url={url} attribution={attribution} eventHandlers={{ load: this._handleTilesReady }} />
              {markers}
            </MapContainer>
          </div>
        )}
      </InteractiveContext.Consumer>
    );
  }
}

export default MapVisualization;
