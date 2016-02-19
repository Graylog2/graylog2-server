import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import MapVisualization from 'components/MapVisualization';
import { MapsActions, MapsStore } from 'stores/MapsStore';

const MapPage = React.createClass({
  mixins: [Reflux.connect(MapsStore)],
  componentDidMount() {
    this._loadData();
    this.interval = setInterval(this._loadData, 5000);
  },
  componentWillUnmount() {
    clearInterval(this.interval);
  },
  _loadData() {
    MapsActions.get('from_geolocation');
  },
  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <MapVisualization id="1" data={this.state.mapCoordinates} config={{}}/>
        </Col>
      </Row>
    );
  },
});

export default MapPage;
