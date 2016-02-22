import React, {PropTypes} from 'react';
import { Map, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import { Spinner } from 'components/common';

import 'leaflet/dist/leaflet.css';

const MapVisualization = React.createClass({
  propTypes: {
    id: PropTypes.string.isRequired,
    data: PropTypes.object,
    config: PropTypes.object.isRequired,
    height: PropTypes.number,
    width: PropTypes.number,
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
    this._onComponentMount();
  },
  _onComponentMount() {
    this.setState({isComponentMounted: true});
  },
  position: [0, 0],
  // Coordinates are given as "lat,long"
  _formatMarker(coordinates, occurrences) {
    const formattedCoordinates = coordinates.split(',').map(component => Number(component));
    return (
      <CircleMarker key={coordinates} center={formattedCoordinates} radius={occurrences * 5 / this.state.zoomLevel} color="#AF2228" fillColor="#D3242B" opacity={0.8}>
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
  render() {
    if (!this.state.isComponentMounted) {
      return <Spinner/>;
    }

    const data = this.props.data.terms;
    const coordinates = Object.keys(data);
    const markers = coordinates.map(aCoordinates => this._formatMarker(aCoordinates, data[aCoordinates]));

    return (
      <Map center={this.position} zoom={this.state.zoomLevel} style={{height: this.props.height, width: this.props.width}} scrollWheelZoom={false}>
        <TileLayer url={`https://api.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=${window.mapConfig.mapboxAccessToken}`}
                   maxZoom={19}
                   attribution={`<a href="http://www.mapbox.com/about/maps/" target="_blank">Terms &amp; Feedback</a>`}/>
        {markers}
      </Map>
    );
  },
});

export default MapVisualization;
