import React from 'react';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';

import { LinkToNode } from 'components/common';
import { LoggingSubsystem, LogLevelMetricsOverview } from 'components/loggers';

import ActionsProvider from 'injection/ActionsProvider';
const MetricsActions = ActionsProvider.getActions('Metrics');

import StoreProvider from 'injection/StoreProvider';
const MetricsStore = StoreProvider.getStore('Metrics');

const NodeLoggers = React.createClass({
  propTypes: {
    nodeId: React.PropTypes.string.isRequired,
    subsystems: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  getInitialState() {
    return { showDetails: false };
  },
  componentDidMount() {
    MetricsActions.add(this.props.nodeId, this.metric_name);
  },
  componentWillUnmount() {
    MetricsActions.remove(this.props.nodeId, this.metric_name);
  },
  metric_name: 'org.apache.logging.log4j.core.Appender.all',
  _formatThroughput() {
    const { metrics } = this.state;
    const { nodeId } = this.props;
    if (metrics && metrics[nodeId] && metrics[nodeId][this.metric_name]) {
      const metric = metrics[nodeId][this.metric_name].metric;
      return metric.rate.total;
    }
    return 'n/a';
  },

  render() {
    const { nodeId } = this.props;
    const subsystems = Object.keys(this.props.subsystems)
      .map(subsystem => <LoggingSubsystem name={subsystem}
                                            nodeId={nodeId}
                                            key={`logging-subsystem-${nodeId}-${subsystem}`}
                                            subsystem={this.props.subsystems[subsystem]} />);

    const logLevelMetrics = <LogLevelMetricsOverview nodeId={this.props.nodeId} />;
    return (
      <Row className="row-sm log-writing-node content">
        <Col md={12}>
          <div style={{ marginBottom: '20' }}>
            <div className="pull-right">
              <Button bsSize="sm" bsStyle="primary" className="trigger-log-level-metrics"
                      onClick={() => this.setState({ showDetails: !this.state.showDetails })}>
                <i className="fa fa-dashboard" />{' '}
                {this.state.showDetails ? 'Hide' : 'Show'} log level metrics
              </Button>
            </div>
            <h2>
              <LinkToNode nodeId={nodeId} />
              <span style={{ fontSize: '12px' }}> Has written a total of <strong>{this._formatThroughput()} internal log messages.</strong></span>
            </h2>
          </div>
          <div className="subsystems">
            {subsystems}
          </div>
          {this.state.showDetails && logLevelMetrics}
        </Col>
      </Row>
    );
  },
});

export default NodeLoggers;
