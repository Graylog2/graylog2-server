import React from 'react';
import styled, { css } from 'styled-components';

import { Metric } from 'components/metrics';
import type { Metric as MetricType } from 'stores/metrics/MetricsStore';

const MetricListWrap = styled.ul(({ theme }) => css`
  padding: 0;

  li {
    margin-bottom: 5px;

    .prefix {
      color: ${theme.colors.gray[70]};
    }

    .name {
      font-size: ${theme.fonts.size.body};
      font-family: ${theme.fonts.family.monospace};
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
`);

export type MetricInfo = {
  type: string,
  full_name: string,
}
type Props = {
  namespace: string,
  nodeId: string,
  names: Array<MetricType>,
}

class MetricsList extends React.Component<Props> {
  _formatMetric = (metric: MetricType) => {
    const { namespace, nodeId } = this.props;

    return (
      <li key={`li-${metric.full_name}`}>
        <Metric key={metric.full_name} metric={metric} namespace={namespace} nodeId={nodeId} />
      </li>
    );
  };

  render() {
    const { names } = this.props;
    const metrics = names
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
