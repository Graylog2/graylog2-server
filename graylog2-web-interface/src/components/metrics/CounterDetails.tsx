import React from 'react';
import numeral from 'numeral';

import type { CounterMetric } from 'stores/metrics/MetricsStore';

type Props = {
  metric: CounterMetric,
}
const CounterDetails = ({ metric: { metric } }: Props) => (
  <dl className="metric-def metric-counter">
    <dt>Value:</dt>
    <dd><span className="number-format">{numeral(metric.count).format('0,0')}</span></dd>
  </dl>
);

export default CounterDetails;
