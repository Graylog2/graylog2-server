import React from 'react';
import numeral from 'numeral';

import type { HistogramMetric } from 'stores/metrics/MetricsStore';

type Props = {
  metric: HistogramMetric,
};
const HistogramDetails = ({ metric: { metric: histogram } }: Props) => (
  <dl className="metric-def metric-histogram">
    <dt>95th percentile:</dt>
    <dd><span className="number-format">{numeral(histogram.time['95th_percentile']).format('0,0.[00]')}</span></dd>
    <dt>98th percentile:</dt>
    <dd><span className="number-format">{numeral(histogram.time['98th_percentile']).format('0,0.[00]')}</span></dd>
    <dt>99th percentile:</dt>
    <dd><span className="number-format">{numeral(histogram.time['99th_percentile']).format('0,0.[00]')}</span></dd>
    <dt>Standard deviation:</dt>
    <dd><span className="number-format">{numeral(histogram.time.std_dev).format('0,0.[00]')}</span></dd>
    <dt>Mean:</dt>
    <dd><span className="number-format">{numeral(histogram.time.mean).format('0,0.[00]')}</span></dd>
    <dt>Minimum:</dt>
    <dd><span className="number-format">{numeral(histogram.time.min).format('0,0.[00]')}</span></dd>
    <dt>Maximum:</dt>
    <dd><span className="number-format">{numeral(histogram.time.max).format('0,0.[00]')}</span></dd>
    <dt>Count:</dt>
    <dd><span className="number-format">{numeral(histogram.count).format('0,0')}</span></dd>
  </dl>
);

export default HistogramDetails;
