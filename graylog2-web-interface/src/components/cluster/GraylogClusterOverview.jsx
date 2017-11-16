import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import { Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

import TrafficGraph from './TrafficGraph';

const ClusterTrafficStore = StoreProvider.getStore('ClusterTraffic');
const ClusterTrafficActions = ActionsProvider.getActions('ClusterTraffic');
const NodesStore = StoreProvider.getStore('Nodes');

const GraylogClusterOverview = React.createClass({
  mixins: [Reflux.connect(NodesStore, 'nodes'), Reflux.connect(ClusterTrafficStore, 'traffic')],

  componentDidMount() {
    ClusterTrafficActions.traffic();
  },

  _isClusterLoading() {
    return !this.state.nodes;
  },

  render() {
    let content = <Spinner />;

    if (!this._isClusterLoading()) {
      content = (
        <dl className="system-dl" style={{ marginBottom: 0 }}>
          <dt>Cluster ID:</dt>
          <dd>{this.state.nodes.clusterId || 'Not available'}</dd>
          <dt>Number of nodes:</dt>
          <dd>{this.state.nodes.nodeCount}</dd>
        </dl>
      );
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2 style={{ marginBottom: 10 }}>Graylog cluster</h2>
          {content}
          <hr />
          <h4 style={{ marginBottom: 10 }}>Outgoing traffic</h4>
          <TrafficGraph traffic={this.state.traffic} />
        </Col>
      </Row>
    );
  },
});

export default GraylogClusterOverview;
