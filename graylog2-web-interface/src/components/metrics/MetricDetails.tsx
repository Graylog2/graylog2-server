/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useEffect } from 'react';
import capitalize from 'lodash/capitalize';
import styled from 'styled-components';

import CounterDetails from 'components/metrics/CounterDetails';
import GaugeDetails from 'components/metrics/GaugeDetails';
import HistogramDetails from 'components/metrics/HistogramDetails';
import MeterDetails from 'components/metrics/MeterDetails';
import TimerDetails from 'components/metrics/TimerDetails';
import { useStore } from 'stores/connect';
import type { Metric } from 'stores/metrics/MetricsStore';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';

const DetailsForType = ({ metric }: { metric: Metric }) => {
  switch (metric.type) {
    case 'counter':
      return <CounterDetails metric={metric} />;
    case 'gauge':
      return <GaugeDetails metric={metric} />;
    case 'histogram':
      return <HistogramDetails metric={metric} />;
    case 'meter':
      return <MeterDetails metric={metric} />;
    case 'timer':
      return <TimerDetails metric={metric} />;
    default:
      return <i>Invalid metric type: {metric}</i>;
  }
};

const StyledMetricDetail = styled.div`
  dl {
    > dt {
      float: left;
    }

    &.metric-timer > dd {
      margin-left: 145px;
    }

    &.metric-meter > dd {
      margin-left: 115px;
    }

    &.metric-gauge > dd {
      margin-left: 90px;
    }

    &.metric-counter > dd {
      margin-left: 90px;
    }

    &.metric-histogram > dd {
      margin-left: 145px;
    }
  }
`;

type Props = {
  metric: Metric,
  nodeId: string,
}

const MetricDetails = ({ nodeId, metric, metric: { full_name: metricName } }: Props) => {
  const metrics = useStore(MetricsStore, (metricsStoreState) => metricsStoreState.metrics);

  useEffect(() => {
    MetricsActions.add(nodeId, metricName);

    return () => { MetricsActions.remove(nodeId, metricName); };
  }, [metricName, nodeId]);

  const currentMetric = metrics?.[nodeId]?.[metricName] ?? metric;
  const type = capitalize(currentMetric.type);

  return (
    <div className="metric">
      <h3>{type}</h3>
      <StyledMetricDetail>
        <DetailsForType metric={currentMetric} />
      </StyledMetricDetail>
    </div>
  );
};

export default MetricDetails;
