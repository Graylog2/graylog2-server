import React from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';
import { Button, ProgressBar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import MetricsStore from 'stores/metrics/MetricsStore';

import MetricsActions from 'actions/metrics/MetricsActions';

import Routes from 'routing/Routes';
import { Spinner } from 'components/common';

const BufferUsage = React.createClass({
  propTypes: {
    bufferType: React.PropTypes.string.isRequired,
    nodeId: React.PropTypes.string.isRequired,
    title: React.PropTypes.node.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  componentWillMount() {
    const metricNames = [
      this._metricPrefix() + 'usage',
      this._metricPrefix() + 'size',
    ];
    metricNames.forEach((metricName) => MetricsActions.add(this.props.nodeId, metricName));
  },
  _metricPrefix() {
    return 'org.graylog2.buffers.' + this.props.bufferType + '.';
  },
  render() {
    if (!this.state.metrics) {
      return <Spinner />;
    }
    const nodeId = this.props.nodeId;
    const usageMetric = this.state.metrics[nodeId][this._metricPrefix() + 'usage'];
    const usagePercentage = usageMetric ? usageMetric.metric.value : NaN;
    const percentLabel = numeral(usagePercentage / 100).format('0.0 %');
    const sizeMetric = this.state.metrics[nodeId][this._metricPrefix() + 'size'];
    const size = sizeMetric ? sizeMetric.metric.value : NaN;
    return (
      <div>
        <LinkContainer to={Routes.SYSTEM.METRIC(nodeId)}>
          <Button bsSize="xsmall" className="pull-right">Metrics</Button>
        </LinkContainer>
        <h3>{this.props.title}</h3>
        <div className="node-buffer-usage">
          <ProgressBar now={usagePercentage}
                       bsStyle="warning"
                       label={percentLabel} />
        </div>
        <span><strong>{size} messages</strong> in {this.props.title.toLowerCase()}, {percentLabel} utilized.</span>
      </div>);
  },
});

export default BufferUsage;
