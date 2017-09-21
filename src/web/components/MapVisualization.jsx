import PropTypes from 'prop-types';
import React from 'react';
import { Map, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import { Spinner } from 'components/common';

const MapVisualization = React.createClass({
  propTypes: {
    id: PropTypes.string.isRequired,
    data: PropTypes.object,
    config: PropTypes.object.isRequired,
    height: PropTypes.number,
    width: PropTypes.number,
    url: PropTypes.string,
    attribution: PropTypes.string,
  },
  getDefaultProps() {
    return {
      data: {},
    };
  },
  getInitialState() {
    return {
      zoomLevel: 1,
      isComponentMounted: false, // Leaflet operates directly with the DOM, so we need to wait until it is ready :grumpy:
    };
  },
  componentDidMount() {
    this.leafletStyle.use();
    this.style.use();
    this._onComponentMount();
  },

  componentWillUnmount() {
    this.leafletStyle.unuse();
    this.style.unuse();
  },

  _onComponentMount() {
    this.setState({isComponentMounted: true});
  },

  leafletStyle: require('!style/useable!css!leaflet/dist/leaflet.css'),
  style: require('!style/useable!css!./MapVisualization.css'),

  position: [0, 0],
  DEFAULT_URL: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
  DEFAULT_ATTRIBUTION: `&copy; <a href="http://osm.org/copyright" target="_blank">OpenStreetMap</a> contributors`,
  MARKER_RADIUS_SIZES: 10,
  MARKER_RADIUS_INCREMENT_SIZES: 10,
  // Coordinates are given as "lat,long"
  _formatMarker(coordinates, occurrences, min, max, increment) {
    const formattedCoordinates = coordinates.split(',').map(component => Number(component));
    const radius = this._getBucket(occurrences, this.MARKER_RADIUS_SIZES, min, max, increment);
    return (
      <CircleMarker key={coordinates} center={formattedCoordinates} radius={radius} color="#AF2228" fillColor="#D3242B" weight={2} opacity={0.8}>
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
    this.setState({zoomLevel: event.target.getZoom()});
  },
  _getBucket(value, bucketCount, minValue, maxValue, increment) {
    // Calculate bucket size based on min/max value and the number of buckets.
    const bucketSize = (maxValue - minValue) / bucketCount;
    // Select bucket for the given value.
    const bucket = value < maxValue ? Math.ceil((value - minValue) / bucketSize) : bucketCount;

    return bucket + increment;
  },
  render() {
    if (!this.state.isComponentMounted) {
      return <Spinner/>;
    }

    const data = this.props.data.terms;
    const occurrences = Object.keys(data).map((k) => data[k]);
    const minOccurrences = occurrences.reduce((prev, cur) => Math.min(prev, cur), Infinity);
    const maxOccurrences = occurrences.reduce((prev, cur) => Math.max(prev, cur), -Infinity);
    const increment = this._getBucket(this.state.zoomLevel, this.MARKER_RADIUS_INCREMENT_SIZES, 1, 10, 1);

    const coordinates = Object.keys(data);
    const markers = coordinates.map(aCoordinates => this._formatMarker(aCoordinates, data[aCoordinates], minOccurrences, maxOccurrences, increment));
    const leafletUrl = this.props.url || this.DEFAULT_URL;
    const leafletAttribution = this.props.attribution || this.DEFAULT_ATTRIBUTION;

    return (
      <Map center={this.position} zoom={this.state.zoomLevel} onZoomend={this._onZoomChange} style={{height: this.props.height, width: this.props.width}} scrollWheelZoom={false}>
        <TileLayer url={leafletUrl}
                   maxZoom={19}
                   attribution={leafletAttribution}/>
        {markers}
      </Map>
    );
  },
});

export default MapVisualization;
