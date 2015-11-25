import React from 'react';
import Reflux from 'reflux';
import String from 'string';

import MetricsStore from 'stores/metrics/MetricsStore';

import MetricsActions from 'actions/metrics/MetricsActions';

import { CounterDetails, GaugeDetails, HistogramDetails, MeterDetails, TimerDetails } from 'components/metrics';

const MetricDetails = React.createClass({
  propTypes: {
    metric: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  componentDidMount() {
    MetricsActions.add(this.props.metric.full_name);
  },
  componentWillUnmount() {
    MetricsActions.remove(this.props.metric.full_name);
  },
  _formatDetailsForType(type, metric) {
    switch(type) {
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
    const metric = this.state.metrics && this.state.metrics[metricName] ? this.state.metrics[metricName] : this.props.metric;
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
