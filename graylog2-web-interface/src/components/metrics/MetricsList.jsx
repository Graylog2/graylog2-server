import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Metric } from 'components/metrics';

const MetricListWrap = styled.ul`
  padding: 0;

  li {
    margin-bottom: 5px;

    .prefix {
      color: #aaa;
    }

    .name {
      font-size: 13px;
      font-family: monospace;
      word-break: break-all;

      .open:hover {
        text-decoration: none;
      }
    }

    .metric {
      margin-left: 10px;
      padding: 10px;

      h3 {
        margin-bottom: 5px;
      }
    }
  }

  dl {
    margin-top: 0;
    margin-bottom: 0;
  }
`;

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
      .map((metric) => this._formatMetric(metric));

    return (
      <MetricListWrap>
        {metrics.length > 0 ? metrics : <li>No metrics match the given filter. Please ensure you use a valid regular expression</li>}
      </MetricListWrap>
    );
  }
}

export default MetricsList;
