import React from 'react';
import numeral from 'numeral';

import type { GaugeMetric } from 'stores/metrics/MetricsStore';

type Props = {
  metric: GaugeMetric,
}
const GaugeDetails = ({ metric: { metric: gauge } }: Props) => (
  <dl className="metric-def metric-gauge">
    <dt>Value:</dt>
    <dd><span className="number-format">{numeral(gauge.value).format('0,0')}</span></dd>
  </dl>
);

export default GaugeDetails;
