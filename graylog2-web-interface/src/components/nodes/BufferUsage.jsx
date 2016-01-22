import React from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';
import { Button, ProgressBar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import MetricsStore from 'stores/metrics/MetricsStore';

import MetricsActions from 'actions/metrics/MetricsActions';

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
    const usage = usageMetric ? usageMetric.metric.value : NaN;
    const sizeMetric = this.state.metrics[nodeId][this._metricPrefix() + 'size'];
    const size = sizeMetric ? sizeMetric.metric.value : NaN;
    const usagePercentage = (!isNaN(usage) && !isNaN(size) ? usage / size : 0);
    const percentLabel = NumberUtils.formatPercentage(usagePercentage);

    return (
      <div>
        <LinkContainer to={Routes.filtered_metrics(nodeId, this._metricPrefix())}>
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
