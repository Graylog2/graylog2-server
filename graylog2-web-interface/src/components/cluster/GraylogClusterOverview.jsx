import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import { Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');

const GraylogClusterOverview = React.createClass({
  mixins: [Reflux.listenTo(NodesStore, '_onNodesChange', '_onNodesChange')],

  getInitialState() {
    return {
      clusterId: undefined,
      nodeCount: 0,
    };
  },

  _onNodesChange() {
    this.setState({ clusterId: NodesStore.getClusterId(), nodeCount: NodesStore.getNodeCount() });
  },

  _isLoading() {
    return !this.state.clusterId;
  },

  render() {
    let content = <Spinner />;

    if (!this._isLoading()) {
      content = (
        <dl className="system-dl" style={{ marginBottom: 0 }}>
          <dt>Cluster ID:</dt>
          <dd>{this.state.clusterId}</dd>
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
