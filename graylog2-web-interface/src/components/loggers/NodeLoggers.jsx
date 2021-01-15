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
import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Col, Row, Button } from 'components/graylog';
import { LinkToNode, IfPermitted, Icon } from 'components/common';
import { LoggingSubsystem, LogLevelMetricsOverview } from 'components/loggers';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';

const MetricsActions = ActionsProvider.getActions('Metrics');
const MetricsStore = StoreProvider.getStore('Metrics');

const NodeLoggers = createReactClass({
  displayName: 'NodeLoggers',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
    subsystems: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  getInitialState() {
    return { showDetails: false };
  },

  componentDidMount() {
    const { nodeId } = this.props;

    MetricsActions.add(nodeId, this.metric_name);
  },

  componentWillUnmount() {
    const { nodeId } = this.props;

    MetricsActions.remove(nodeId, this.metric_name);
  },

  metric_name: 'org.apache.logging.log4j.core.Appender.all',

  _formatThroughput() {
    const { metrics } = this.state;
    const { nodeId } = this.props;

    if (metrics && metrics[nodeId] && metrics[nodeId][this.metric_name]) {
      const { metric } = metrics[nodeId][this.metric_name];

      return metric.rate.total;
    }

    return 'n/a';
  },

  render() {
    const { nodeId, subsystems } = this.props;
    const { showDetails } = this.state;
    const subsystemKeys = Object.keys(subsystems)
      .map((subsystem) => (
        <LoggingSubsystem name={subsystem}
                          nodeId={nodeId}
                          key={`logging-subsystem-${nodeId}-${subsystem}`}
                          subsystem={subsystems[subsystem]} />
      ));

    const logLevelMetrics = <LogLevelMetricsOverview nodeId={nodeId} />;

    return (
      <Row className="row-sm log-writing-node content">
        <Col md={12}>
          <IfPermitted permissions="loggers:read">
            <div style={{ marginBottom: '20' }}>
              <div className="pull-right">
                <Button bsSize="sm"
                        bsStyle="primary"
                        className="trigger-log-level-metrics"
                        onClick={() => this.setState({ showDetails: !showDetails })}>
                  <Icon name="tachometer-alt" />{' '}
                  {showDetails ? 'Hide' : 'Show'} log level metrics
                </Button>
              </div>
              <h2>
                <LinkToNode nodeId={nodeId} />
                <small> Has written a total of <strong>{this._formatThroughput()} internal log messages.</strong></small>
              </h2>
            </div>
            <div className="subsystems">
              {subsystemKeys}
            </div>
            {showDetails && logLevelMetrics}
          </IfPermitted>
        </Col>
      </Row>
    );
  },
});

export default NodeLoggers;
