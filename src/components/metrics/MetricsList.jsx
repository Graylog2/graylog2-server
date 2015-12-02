import React from 'react';

import { Spinner } from 'components/common';
import { Metric } from 'components/metrics';

const MetricsList = React.createClass({
  propTypes: {
    names: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    namespace: React.PropTypes.string.isRequired,
    nodeId: React.PropTypes.string.isRequired,
  },
  _formatMetric(metric) {
    return (
      <li key={'li-' + metric.full_name}>
        <Metric key={metric.full_name} metric={metric} namespace={this.props.namespace} nodeId={this.props.nodeId}/>
      </li>
    );
  },
  render() {
    return (
      <ul className="metric-list">
        {this.props.names.sort((m1, m2) => m1.full_name.localeCompare(m2.full_name))
          .map((metric) => this._formatMetric(metric))}
      </ul>
    );
  },
});

export default MetricsList;
