/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
/* eslint-disable react/no-find-dom-node */
/* global window */
import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import ReactDOM from 'react-dom';
import Reflux from 'reflux';
import _ from 'lodash';

import { Col, Row } from 'components/graylog';
import { Spinner } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import NumberUtils from 'util/NumberUtils';
import EventHandlersThrottler from 'util/EventHandlersThrottler';

import TrafficGraph from './TrafficGraph';

const ClusterTrafficStore = StoreProvider.getStore('ClusterTraffic');
const ClusterTrafficActions = ActionsProvider.getActions('ClusterTraffic');
const NodesStore = StoreProvider.getStore('Nodes');

const GraylogClusterOverview = createReactClass({
  displayName: 'GraylogClusterOverview',

  propTypes: {
    layout: PropTypes.oneOf(['default', 'compact']),
    children: PropTypes.node,
  },

  mixins: [Reflux.connect(NodesStore, 'nodes'), Reflux.connect(ClusterTrafficStore, 'traffic')],

  getDefaultProps() {
    return {
      layout: 'default',
      children: null,
    };
  },

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

  renderClusterInfo() {
    const { nodes } = this.state;

    let content = <Spinner />;

    if (nodes) {
      content = (
        <dl className="system-dl" style={{ marginBottom: 0 }}>
          <dt>Cluster ID:</dt>
          <dd>{nodes.clusterId || 'Not available'}</dd>
          <dt>Number of nodes:</dt>
          <dd>{nodes.nodeCount}</dd>
        </dl>
      );
    }

    return content;
  },

  renderTrafficGraph() {
    const { traffic, graphWidth } = this.state;

    let sumOutput = null;
    let trafficGraph = <Spinner />;

    if (traffic) {
      const bytesOut = _.reduce(traffic.output, (result, value) => result + value);

      sumOutput = <small>Last 30 days: {NumberUtils.formatBytes(bytesOut)}</small>;

      trafficGraph = (
        <TrafficGraph traffic={traffic.output}
                      from={traffic.from}
                      to={traffic.to}
                      width={graphWidth} />
      );
    }

    return (
      <>
        <h3 ref={(container) => { this._container = container; }} style={{ marginBottom: 10 }}>Outgoing traffic {sumOutput}</h3>
        {trafficGraph}
      </>
    );
  },

  renderHeader() {
    return <h2 style={{ marginBottom: 10 }}>Graylog cluster</h2>;
  },

  renderDefaultLayout() {
    const { children } = this.props;

    return (
      <Row className="content">
        <Col md={12}>
          {this.renderHeader()}
          {this.renderClusterInfo()}
          <hr />
          {children}
          <Row>
            <Col md={12}>
              {this.renderTrafficGraph()}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },

  renderCompactLayout() {
    const { children } = this.props;

    return (
      <Row className="content">
        <Col md={12}>
          {this.renderHeader()}
          <Row>
            <Col md={6}>
              {this.renderClusterInfo()}
              <hr />
              {children}
            </Col>
            <Col md={6}>
              {this.renderTrafficGraph()}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },

  render() {
    const { layout } = this.props;

    switch (layout) {
      case 'compact':
        return this.renderCompactLayout();
      default:
        return this.renderDefaultLayout();
    }
  },
});

export default GraylogClusterOverview;
