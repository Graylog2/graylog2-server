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

import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import type { Stream } from 'stores/streams/StreamsStore';
import { Spinner } from 'components/common';
import { useStore } from 'stores/connect';

type Props = {
  stream: Stream,
};

const ThroughputCell = ({ stream }: Props) => {
  const { metrics } = useStore(MetricsStore);
  const metricName = `org.graylog2.plugin.streams.Stream.${stream.id}.incomingMessages.1-sec-rate`;

  useEffect(() => {
    MetricsActions.addGlobal(metricName);

    return () => {
      MetricsActions.removeGlobal(metricName);
    };
  });

  if (!metrics) {
    return <Spinner />;
  }

  const throughput = Object.keys(metrics)
    .map((nodeId) => {
      const metricDefinition = metrics[nodeId]?.[metricName];

      return (metricDefinition?.metric && 'value' in metricDefinition.metric) ? metricDefinition.metric.value : 0;
    })
    .reduce((throughput1, throughput2) => throughput1 + throughput2, 0);

  return (<>{throughput} msg/s</>);
};

export default ThroughputCell;
