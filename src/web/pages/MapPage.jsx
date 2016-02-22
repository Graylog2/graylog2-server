import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import MapVisualization from 'components/MapVisualization';
import { Spinner } from 'components/common';
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
    if (!this.state.mapCoordinates) {
      return <Spinner/>;
    }

    return (
      <Row className="content">
        <Col md={12}>
          <MapVisualization id="1" data={this.state.mapCoordinates} height={600} width={1200} config={{}}/>
        </Col>
      </Row>
    );
  },
});

export default MapPage;
