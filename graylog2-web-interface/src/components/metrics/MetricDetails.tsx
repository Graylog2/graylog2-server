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
import PropTypes from 'prop-types';
import capitalize from 'lodash/capitalize';
import styled from 'styled-components';

import { CounterDetails, GaugeDetails, HistogramDetails, MeterDetails, TimerDetails } from 'components/metrics';
import { useStore } from 'stores/connect';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import type { MetricInfo } from 'components/metrics/MetricsList';

const DetailsForType = ({ type, metric }: { type: string, metric: MetricInfo }) => {
  switch (type) {
    case 'Counter':
      return <CounterDetails metric={metric} />;
    case 'Gauge':
      return <GaugeDetails metric={metric} />;
    case 'Histogram':
      return <HistogramDetails metric={metric} />;
    case 'Meter':
      return <MeterDetails metric={metric} />;
    case 'Timer':
      return <TimerDetails metric={metric} />;
    default:
      return <i>Invalid metric type: {type}</i>;
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
  metric: MetricInfo,
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
        <DetailsForType type={type} metric={currentMetric} />
      </StyledMetricDetail>
    </div>
  );
};

MetricDetails.propTypes = {
  metric: PropTypes.object.isRequired,
  nodeId: PropTypes.string.isRequired,
};

export default MetricDetails;
