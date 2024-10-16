import React, { useCallback, useState } from 'react';

import { MetricDetails } from 'components/metrics';
import { Icon } from 'components/common';
import type { Metric as MetricType } from 'stores/metrics/MetricsStore';

const iconMapping = {
  timer: 'schedule',
  histogram: 'signal_cellular_alt',
  meter: 'play_circle',
  gauge: 'speed',
  counter: 'circle',
  unknown: 'help',
};

const _formatIcon = (type: string) => iconMapping[type] ?? iconMapping.unknown;

const _formatName = (metricName: string, namespace: string) => {
  const split = metricName.split(namespace);
  const unqualifiedMetricName = split.slice(1).join(namespace);

  return (
    <span>
      <span className="prefix">{namespace}</span>
      {unqualifiedMetricName}
    </span>
  );
};

type Props = {
  metric: MetricType,
  namespace: string,
  nodeId: string,
}

const Metric = ({ metric, namespace, nodeId }: Props) => {
  const [expanded, setExpanded] = useState(false);
  const _showDetails = useCallback((e) => {
    e.preventDefault();
    setExpanded((_expanded) => !_expanded);
  }, []);

  const details = expanded ? <MetricDetails nodeId={nodeId} metric={metric} /> : null;

  return (
    <span>
      <div className="name">
        <Icon name={_formatIcon(metric.type)} />{' '}
        {/* eslint-disable-next-line jsx-a11y/anchor-is-valid */}
        <a className="open" href="#" onClick={_showDetails}>{_formatName(metric.full_name, namespace)}</a>
      </div>
      {details}
    </span>
  );
};

export default Metric;
