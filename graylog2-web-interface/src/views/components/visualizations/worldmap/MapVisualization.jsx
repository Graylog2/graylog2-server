/* eslint-env browser */
import PropTypes from 'prop-types';
import React from 'react';
import { CircleMarker, Map, Popup, TileLayer } from 'react-leaflet';
import chroma from 'chroma-js';
import { flatten } from 'lodash';

import style from './MapVisualization.css';

// eslint-disable-next-line import/no-webpack-loader-syntax
import leafletStyles from '!style/useable!css!leaflet/dist/leaflet.css';

const DEFAULT_VIEWPORT = {
  center: [0, 0],
  zoom: 1,
};

class MapVisualization extends React.Component {
  _map = undefined

  _isMapReady = false

  _areTilesReady = false

  static propTypes = {
    id: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(PropTypes.object),
    height: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
    url: PropTypes.string,
    attribution: PropTypes.string,
    interactive: PropTypes.bool,
    onRenderComplete: PropTypes.func,
    onChange: PropTypes.func.isRequired,
    locked: PropTypes.bool, // Disables zoom and dragging
    markerRadiusSize: PropTypes.number,
    markerRadiusIncrementSize: PropTypes.number,
    viewport: PropTypes.shape({
      center: PropTypes.arrayOf(PropTypes.number),
      zoom: PropTypes.number,
    }),
  }

  static defaultProps = {
    data: {},
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="http://osm.org/copyright" target="_blank">OpenStreetMap</a> contributors',
    interactive: true,
    onRenderComplete: () => {},
    locked: false,
    viewport: DEFAULT_VIEWPORT,
    markerRadiusSize: 10,
    markerRadiusIncrementSize: 10,
  }

  componentDidMount() {
    this._forceMapUpdate();
    leafletStyles.use();
  }

  componentDidUpdate(prevProps) {
    const { height, width } = this.props;
    if (height !== prevProps.height || width !== prevProps.width) {
      this._forceMapUpdate();
    }
  }

  componentWillUnmount() {
    leafletStyles.unuse();
  }

  // Workaround to avoid wrong placed markers or empty tiles if the map container size changed.
  _forceMapUpdate = () => {
    if (this._map) {
      window.dispatchEvent(new Event('resize'));
      const { interactive } = this.props;
      this._map.leafletElement.invalidateSize(interactive);
    }
  }

  // Coordinates are given as "lat,long"
  _formatMarker = (coordinates, value, min, max, radiusSize, increment, color, name, keys) => {
    // eslint-disable-next-line no-restricted-globals
    const formattedCoordinates = coordinates.split(',').map(component => Number(component)).filter(n => !isNaN(n));
    if (formattedCoordinates.length !== 2) {
      return null;
    }
    const radius = this._getBucket(value, radiusSize, min, max, increment);
    const markerKeys = flatten(Object.entries(keys).map(([k, v]) => [<dt key={`dt-${k}-${v}`}>{k}</dt>, <dd key={`dd-${k}-${v}`}>{v}</dd>]));
    return (
      <CircleMarker key={`${name}-${coordinates}`}
                    center={formattedCoordinates}
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
              <React.Fragment>
                <dt>Value:</dt>
                <dd>{value}</dd>
              </React.Fragment>
              )
            }
          </dl>
        </Popup>
      </CircleMarker>
    );
  }

  _getBucket = (value, bucketCount, minValue, maxValue, increment) => {
    // Calculate bucket size based on min/max value and the number of buckets.
    const bucketSize = (maxValue - minValue) / bucketCount;
    // Select bucket for the given value.
    const bucket = value < maxValue ? Math.ceil((value - minValue) / bucketSize) : bucketCount;

    return bucket + increment;
  }

  _handleRenderComplete = () => {
    if (this._areTilesReady && this._isMapReady) {
      const { onRenderComplete } = this.props;
      onRenderComplete();
    }
  }

  _handleMapReady = () => {
    this._isMapReady = true;
    this._handleRenderComplete();
  }

  _handleTilesReady = () => {
    this._areTilesReady = true;
    this._handleRenderComplete();
  }

  render() {
    const { data, id, height, width, url, attribution, interactive, locked, viewport, onChange, markerRadiusSize, markerRadiusIncrementSize } = this.props;

    const noOfKeys = data.length;
    const chromaScale = chroma.scale('Spectral');
    const markers = [];
    data.forEach(({ keys, name, values }, idx) => {
      const y = Object.values(values);
      const min = Math.min(...y);
      const max = Math.max(...y);
      const color = chromaScale(idx * (1 / noOfKeys));
      Object.entries(values)
        .forEach(([coord, value], valueIdx) => markers
          .push(this._formatMarker(coord, value, min, max, markerRadiusSize, markerRadiusIncrementSize, color, name, keys[valueIdx])));
    });

    return (
      <div className={locked ? style.mapLocked : ''} style={{ position: 'relative', zIndex: 0 }}>
        {locked && <div className={style.overlay} style={{ height, width }} />}
        <Map ref={(c) => { this._map = c; }}
             id={`visualization-${id}`}
             viewport={viewport}
             onViewportChanged={onChange}
             className={style.map}
             style={{ height, width }}
             scrollWheelZoom
             animate={interactive}
             zoomAnimation={interactive}
             fadeAnimation={interactive}
             markerZoomAnimation={interactive}
             whenReady={this._handleMapReady}>
          <TileLayer url={url} maxZoom={19} attribution={attribution} onLoad={this._handleTilesReady} />
          {markers}
        </Map>
      </div>
    );
  }
}


export default MapVisualization;
