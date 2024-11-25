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
import React, { useEffect } from 'react';
import capitalize from 'lodash/capitalize';
import numeral from 'numeral';

import { Col } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';

type Props = {
  nodeId: string,
  loglevel: string,
}

const LogLevelMetrics = ({ nodeId, loglevel }: Props) => {
  const { metrics } = useStore(MetricsStore);
  const metricName = `org.apache.logging.log4j.core.Appender.${loglevel}`;

  useEffect(() => {
    MetricsActions.add(nodeId, metricName);

    return () => { MetricsActions.remove(nodeId, metricName); };
  }, [metricName, nodeId]);

  let metricsDetails;

  if (!metrics?.[nodeId]?.[metricName]) {
    metricsDetails = <Spinner />;
  } else {
    const { metric } = metrics[nodeId][metricName];

    metricsDetails = 'rate' in metric ? (
      <dl className="loglevel-metrics-list">
        <dt>Total written:</dt>
        <dd><span className="loglevel-metric-total">{metric.rate.total}</span></dd>
        <dt>Mean rate:</dt>
        <dd><span className="loglevel-metric-mean">{numeral(metric.rate.mean).format('0.00')}</span> / second</dd>
        <dt>1 min rate:</dt>
        <dd><span className="loglevel-metric-1min">{numeral(metric.rate.one_minute).format('0.00')}</span> / second</dd>
      </dl>
    ) : null;
  }

  return (
    <div className="loglevel-metrics-row">
      <Col md={4}>
        <h3 className="u-light">Level: {capitalize(loglevel)}</h3>
        {metricsDetails}
      </Col>
    </div>
  );
};

export default LogLevelMetrics;
