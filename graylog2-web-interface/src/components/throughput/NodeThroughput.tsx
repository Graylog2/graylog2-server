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
import numeral from 'numeral';

import { Spinner } from 'components/common';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';

type Props = {
  nodeId: string,
  longFormat?: boolean,
}
const metricNames = {
  totalIn: 'org.graylog2.throughput.input.1-sec-rate',
  totalOut: 'org.graylog2.throughput.output.1-sec-rate',
};

// TODO this is a copy of GlobalThroughput, it just renders differently and only targets a single node.
const NodeThroughput = ({ nodeId, longFormat = false }: Props) => {
  const { metrics: _metrics } = useStore(MetricsStore);

  useEffect(() => {
    Object.keys(metricNames).forEach((metricShortName) => MetricsActions.add(nodeId, metricNames[metricShortName]));

    return () => {
      Object.keys(metricNames).forEach((metricShortName) => MetricsActions.remove(nodeId, metricNames[metricShortName]));
    };
  }, [nodeId]);

  const _isLoading = !_metrics;

  if (_isLoading) {
    return <Spinner text="Loading throughput..." />;
  }

  const nodeMetrics = _metrics[nodeId];
  const metrics = MetricsExtractor.getValuesForNode(nodeMetrics, metricNames);

  if (Object.keys(metrics).length === 0) {
    return (<span>Unable to load throughput.</span>);
  }

  if (longFormat) {
    return (
      <span>
        Processing <strong>{numeral(metrics.totalIn).format('0,0')}</strong> incoming and <strong>
          {numeral(metrics.totalOut).format('0,0')}
                                                                                          </strong> outgoing msg/s.
      </span>
    );
  }

  return (
    <span>
      In {numeral(metrics.totalIn).format('0,0')} / Out {numeral(metrics.totalOut).format('0,0')} msg/s.
    </span>
  );
};

export default NodeThroughput;
