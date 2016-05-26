import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import { Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');

const GraylogClusterOverview = React.createClass({
  mixins: [Reflux.connect(NodesStore)],

  _isLoading() {
    return !this.state.nodes;
  },

  render() {
    let content = <Spinner />;

    if (!this._isLoading()) {
      content = (
        <dl className="system-dl" style={{ marginBottom: 0 }}>
          <dt>Cluster ID:</dt>
          <dd>{this.state.clusterId || 'Not available'}</dd>
          <dt>Number of nodes:</dt>
          <dd>{this.state.nodeCount}</dd>
        </dl>
      );
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2 style={{ marginBottom: 10 }}>Graylog cluster</h2>
          {content}
        </Col>
      </Row>
    );
  },
});

export default GraylogClusterOverview;
