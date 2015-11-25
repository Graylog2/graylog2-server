import React from 'react';
import Reflux from 'reflux';

import MetricsStore from 'stores/metrics/MetricsStore';

import { Spinner } from 'components/common';
import { Metric } from 'components/metrics';

const MetricsList = React.createClass({
  mixins: [Reflux.connect(MetricsStore)],
  _formatMetric(metric) {
    return (
      <li key={'li-' + metric.full_name} data-metricname="@name">
        <Metric key={metric.full_name} metric={metric} namespace={MetricsStore.namespace} />
      </li>
    );
  },
  render() {
    if (!this.state.names) {
      return <Spinner />;
    }

    return (
      <ul className="metric-list">
        {this.state.names.sort((m1, m2) => m1.full_name.localeCompare(m2.full_name))
          .map((metric) => this._formatMetric(metric))}
      </ul>
    );
  },
});

export default MetricsList;
