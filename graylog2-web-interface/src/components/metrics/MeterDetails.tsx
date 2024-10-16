import React from 'react';
import numeral from 'numeral';

import type { MeterMetric } from 'stores/metrics/MetricsStore';

type Props = {
  metric: MeterMetric,
}
const MeterDetails = ({ metric: { metric: meter } }: Props) => (
  <dl className="metric-def metric-meter">
    <dt>Total:</dt>
    <dd><span className="number-format">{numeral(meter.rate.total).format('0,0')}</span> events</dd>
    <dt>Mean:</dt>
    <dd><span className="number-format">{numeral(meter.rate.mean).format('0,0.[00]')}</span> {meter.rate_unit}</dd>
    <dt>1 minute avg:</dt>
    <dd><span className="number-format">{numeral(meter.rate.one_minute).format('0,0.[00]')}</span> {meter.rate_unit}</dd>
    <dt>5 minute avg:</dt>
    <dd><span className="number-format">{numeral(meter.rate.five_minute).format('0,0.[00]')}</span> {meter.rate_unit}</dd>
    <dt>15 minute avg:</dt>
    <dd><span className="number-format">{numeral(meter.rate.fifteen_minute).format('0,0.[00]')}</span> {meter.rate_unit}</dd>
  </dl>
);

export default MeterDetails;
