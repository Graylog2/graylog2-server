/* eslint-disable react/no-find-dom-node */
/* global window */
import React from 'react';
import createReactClass from 'create-react-class';
import ReactDOM from 'react-dom';
import Reflux from 'reflux';
import { Col, Row } from 'components/graylog';

import { Spinner } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import NumberUtils from 'util/NumberUtils';
import _ from 'lodash';
import EventHandlersThrottler from 'util/EventHandlersThrottler';

import TrafficGraph from './TrafficGraph';

const ClusterTrafficStore = StoreProvider.getStore('ClusterTraffic');
const ClusterTrafficActions = ActionsProvider.getActions('ClusterTraffic');
const NodesStore = StoreProvider.getStore('Nodes');

const GraylogClusterOverview = createReactClass({
  displayName: 'GraylogClusterOverview',
  mixins: [Reflux.connect(NodesStore, 'nodes'), Reflux.connect(ClusterTrafficStore, 'traffic')],

  getInitialState() {
    return {
      graphWidth: 600,
    };
  },

  componentDidMount() {
    ClusterTrafficActions.traffic();
    window.addEventListener('resize', this._onResize);
    this._resizeGraphs();
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._onResize);
  },

  eventThrottler: new EventHandlersThrottler(),

  _onResize() {
    this.eventThrottler.throttle(() => this._resizeGraphs());
  },

  _resizeGraphs() {
    const domNode = ReactDOM.findDOMNode(this._container);
    this.setState({ graphWidth: domNode.clientWidth });
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
    let sumOutput = null;
    if (this.state.traffic) {
      const bytesOut = _.reduce(this.state.traffic.output, (result, value) => result + value);
      sumOutput = <small>Last 30 days: {NumberUtils.formatBytes(bytesOut)}</small>;
    }
    return (
      <Row className="content">
        <Col md={12}>
          <h2 style={{ marginBottom: 10 }}>Graylog cluster</h2>
          {content}
          <hr />
          <Row>
            <Col md={12}>
              <h3 ref={(container) => { this._container = container; }} style={{ marginBottom: 10 }}>Outgoing traffic {sumOutput}</h3>
              {!this.state.traffic ? <Spinner /> : (
                <TrafficGraph traffic={this.state.traffic.output}
                              from={this.state.traffic.from}
                              to={this.state.traffic.to}
                              width={this.state.graphWidth} />
              )}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default GraylogClusterOverview;
