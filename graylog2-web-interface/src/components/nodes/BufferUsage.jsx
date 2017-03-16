import React from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';
import { Button, ProgressBar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const MetricsStore = StoreProvider.getStore('Metrics');

import ActionsProvider from 'injection/ActionsProvider';
const MetricsActions = ActionsProvider.getActions('Metrics');

import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';
import { Spinner } from 'components/common';

const BufferUsage = React.createClass({
  propTypes: {
    bufferType: React.PropTypes.string.isRequired,
    nodeId: React.PropTypes.string.isRequired,
    title: React.PropTypes.node.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  componentWillMount() {
    const prefix = this._metricPrefix();
    const metricNames = [
      `${prefix}.usage`,
      `${prefix}.size`,
    ];
    metricNames.forEach(metricName => MetricsActions.add(this.props.nodeId, metricName));
  },
  _metricPrefix() {
    return `org.graylog2.buffers.${this.props.bufferType}`;
  },
  _metricFilter() {
    return `org\\.graylog2\\.buffers\\.${this.props.bufferType}\\.|${this.props.bufferType}buffer`;
  },
  render() {
    if (!this.state.metrics) {
      return <Spinner />;
    }
    const nodeId = this.props.nodeId;
    const prefix = this._metricPrefix();
    const usageMetric = this.state.metrics[nodeId][`${prefix}.usage`];
    const usage = usageMetric ? usageMetric.metric.value : NaN;
    const sizeMetric = this.state.metrics[nodeId][`${prefix}.size`];
    const size = sizeMetric ? sizeMetric.metric.value : NaN;
    const usagePercentage = (!isNaN(usage) && !isNaN(size) ? usage / size : 0);
    const percentLabel = NumberUtils.formatPercentage(usagePercentage);

    return (
      <div>
        <LinkContainer to={Routes.filtered_metrics(nodeId, this._metricFilter())}>
          <Button bsSize="xsmall" className="pull-right">Metrics</Button>
        </LinkContainer>
        <h3>{this.props.title}</h3>
        <div className="node-buffer-usage">
          <ProgressBar now={usagePercentage * 100}
                       bsStyle="warning"
                       label={percentLabel} />
        </div>
        <span><strong>{usage} messages</strong> in {this.props.title.toLowerCase()}, {percentLabel} utilized.</span>
      </div>);
  },
});

export default BufferUsage;
