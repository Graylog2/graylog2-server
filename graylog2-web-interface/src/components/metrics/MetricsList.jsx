import PropTypes from 'prop-types';
import React from 'react';

import { Metric } from 'components/metrics';

class MetricsList extends React.Component {
  static propTypes = {
    names: PropTypes.arrayOf(PropTypes.object).isRequired,
    namespace: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
  };

  _formatMetric = (metric) => {
    return (
      <li key={`li-${metric.full_name}`}>
        <Metric key={metric.full_name} metric={metric} namespace={this.props.namespace} nodeId={this.props.nodeId} />
      </li>
    );
  };

  render() {
    const metrics = this.props.names
      .sort((m1, m2) => m1.full_name.localeCompare(m2.full_name))
      .map(metric => this._formatMetric(metric));

    return (
      <ul className="metric-list">
        {metrics.length > 0 ? metrics : <li>No metrics match the given filter. Please ensure you use a valid regular expression</li>}
      </ul>
    );
  }
}

export default MetricsList;
