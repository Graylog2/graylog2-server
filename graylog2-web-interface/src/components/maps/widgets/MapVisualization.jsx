/* eslint-env browser */
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Map, TileLayer, CircleMarker, Popup } from 'react-leaflet';

import {} from 'leaflet/dist/leaflet.css';
import style from './MapVisualization.css';

const MapVisualization = createReactClass({
  displayName: 'MapVisualization',

  propTypes: {
    id: PropTypes.string.isRequired,
    data: PropTypes.object,
    height: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
    url: PropTypes.string,
    attribution: PropTypes.string,
    interactive: PropTypes.bool,
    onRenderComplete: PropTypes.func,
    locked: PropTypes.bool, // Disables zoom and dragging
  },

  getDefaultProps() {
    return {
      data: {},
      url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      attribution: '&copy; <a href="http://osm.org/copyright" target="_blank">OpenStreetMap</a> contributors',
      interactive: true,
      onRenderComplete: () => {},
      locked: false,
    };
  },

  getInitialState() {
    return {
      zoomLevel: 1,
    };
  },

  componentDidUpdate(prevProps) {
    if (this.props.height !== prevProps.height || this.props.width !== prevProps.width) {
      this._forceMapUpdate();
    }
  },

  _map: undefined,
  _isMapReady: false,
  _areTilesReady: false,
  position: [0, 0],
  MARKER_RADIUS_SIZES: 10,
  MARKER_RADIUS_INCREMENT_SIZES: 10,

  // Workaround to avoid wrong placed markers or empty tiles if the map container size changed.
  _forceMapUpdate() {
    if (this._map) {
      window.dispatchEvent(new Event('resize'));
      this._map.leafletElement.invalidateSize(this.props.interactive);
    }
  },

  // Coordinates are given as "lat,long"
  _formatMarker(coordinates, occurrences, min, max, increment) {
    const formattedCoordinates = coordinates.split(',').map(component => Number(component));
    const radius = this._getBucket(occurrences, this.MARKER_RADIUS_SIZES, min, max, increment);
    return (
      <CircleMarker key={coordinates}
                    center={formattedCoordinates}
                    radius={radius}
                    color="#AF2228"
                    fillColor="#D3242B"
                    weight={2}
                    opacity={0.8}>
        <Popup>
          <dl>
            <dt>Coordinates:</dt>
            <dd>{coordinates}</dd>
            <dt>Number of occurrences:</dt>
            <dd>{occurrences}</dd>
          </dl>
        </Popup>
      </CircleMarker>
    );
  },

  _onZoomChange(event) {
    this.setState({ zoomLevel: event.target.getZoom() });
  },

  _getBucket(value, bucketCount, minValue, maxValue, increment) {
    // Calculate bucket size based on min/max value and the number of buckets.
    const bucketSize = (maxValue - minValue) / bucketCount;
    // Select bucket for the given value.
    const bucket = value < maxValue ? Math.ceil((value - minValue) / bucketSize) : bucketCount;

    return bucket + increment;
  },

  _handleRenderComplete() {
    if (this._areTilesReady && this._isMapReady) {
      this.props.onRenderComplete();
    }
  },

  _handleMapReady() {
    this._isMapReady = true;
    this._handleRenderComplete();
  },

  _handleTilesReady() {
    this._areTilesReady = true;
    this._handleRenderComplete();
  },

  render() {
    const { data, id, height, width, url, attribution, interactive, locked } = this.props;

    const terms = data.terms;
    const occurrences = Object.keys(terms).map(k => terms[k]);
    const minOccurrences = occurrences.reduce((prev, cur) => Math.min(prev, cur), Infinity);
    const maxOccurrences = occurrences.reduce((prev, cur) => Math.max(prev, cur), -Infinity);
    const increment = this._getBucket(this.state.zoomLevel, this.MARKER_RADIUS_INCREMENT_SIZES, 1, 10, 1);

    const coordinates = Object.keys(terms);
    const markers = coordinates.map(aCoordinates => this._formatMarker(aCoordinates, terms[aCoordinates], minOccurrences, maxOccurrences, increment));

    return (
      <div className={locked ? style.mapLocked : ''}>
        {locked && <div className={style.overlay} style={{ height: height, width: width }} />}
        <Map ref={(c) => { this._map = c; }}
             id={`visualization-${id}`}
             center={this.position}
             zoom={this.state.zoomLevel}
             onZoomend={this._onZoomChange}
             className={style.map}
             style={{ height: height, width: width }}
             scrollWheelZoom={false}
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
  },
});

export default MapVisualization;
