import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import lodash from 'lodash';

import { CounterDetails, GaugeDetails, HistogramDetails, MeterDetails, TimerDetails } from 'components/metrics';
import CombinedProvider from 'injection/CombinedProvider';

const { MetricsStore, MetricsActions } = CombinedProvider.get('Metrics');

const MetricDetails = createReactClass({
  displayName: 'MetricDetails',

  propTypes: {
    metric: PropTypes.object.isRequired,
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
    const type = lodash.capitalize(metric.type);
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
