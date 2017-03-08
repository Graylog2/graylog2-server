import React from 'react';
import Reflux from 'reflux';
import String from 'string';

import StoreProvider from 'injection/StoreProvider';
const MetricsStore = StoreProvider.getStore('Metrics');

import ActionsProvider from 'injection/ActionsProvider';
const MetricsActions = ActionsProvider.getActions('Metrics');

import { CounterDetails, GaugeDetails, HistogramDetails, MeterDetails, TimerDetails } from 'components/metrics';

const MetricDetails = React.createClass({
  propTypes: {
    metric: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  componentDidMount() {
    MetricsActions.add(this.props.nodeId, this.props.metric.full_name);
  },
  componentWillUnmount() {
    MetricsActions.remove(this.props.nodeId, this.props.metric.full_name);
  },
  _formatDetailsForType(type, metric) {
    switch (type) {
      case 'Counter':
        return <CounterDetails metric={metric} />;
      case 'Gauge':
        return <GaugeDetails metric={metric} />;
      case 'Histogram':
        return <HistogramDetails metric={metric} />;
      case 'Meter':
        return <MeterDetails metric={metric} />;
      case 'Timer':
        return <TimerDetails metric={metric} />;
      default:
        return <i>Invalid metric type: {type}</i>;
    }
  },
  render() {
    const metricName = this.props.metric.full_name;
    const nodeId = this.props.nodeId;
    const metric = this.state.metrics && this.state.metrics[nodeId] && this.state.metrics[nodeId][metricName] ?
      this.state.metrics[nodeId][metricName] : this.props.metric;
    const type = String(metric.type).capitalize().toString();
    const details = this._formatDetailsForType(type, metric);
    return (
      <div className="metric">
        <h3>{type}</h3>

        {details}
      </div>
    );
  },
});

export default MetricDetails;
