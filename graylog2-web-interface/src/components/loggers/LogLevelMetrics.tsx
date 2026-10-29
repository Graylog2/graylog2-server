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
import React from 'react';
import capitalize from 'lodash/capitalize';
import { useTranslation } from 'react-i18next';
import numeral from 'numeral';

import { Col } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { useNodeMetric } from 'hooks/useMetrics';

type Props = {
  nodeId: string;
  loglevel: string;
};

const LogLevelMetrics = ({ nodeId, loglevel }: Props) => {
  const { t } = useTranslation('system');
  const metricName = `org.apache.logging.log4j.core.Appender.${loglevel}`;
  const { data: nodeMetrics } = useNodeMetric(nodeId, metricName);

  let metricsDetails;

  if (!nodeMetrics?.[metricName]) {
    metricsDetails = <Spinner />;
  } else {
    const { metric } = nodeMetrics[metricName];

    metricsDetails =
      'rate' in metric ? (
        <dl className="loglevel-metrics-list">
          <dt>{t('log_level_metrics.total_written')}</dt>
          <dd>
            <span className="loglevel-metric-total">{metric.rate.total}</span>
          </dd>
          <dt>{t('log_level_metrics.mean_rate')}</dt>
          <dd>
            <span className="loglevel-metric-mean">{numeral(metric.rate.mean).format('0.00')}</span>{' '}
            {t('log_level_metrics.per_second')}
          </dd>
          <dt>{t('log_level_metrics.one_minute_rate')}</dt>
          <dd>
            <span className="loglevel-metric-1min">{numeral(metric.rate.one_minute).format('0.00')}</span>{' '}
            {t('log_level_metrics.per_second')}
          </dd>
        </dl>
      ) : null;
  }

  return (
    <div className="loglevel-metrics-row">
      <Col md={4}>
        <h3 className="u-light">{t('log_level_metrics.level', { level: capitalize(loglevel) })}</h3>
        {metricsDetails}
      </Col>
    </div>
  );
};

export default LogLevelMetrics;
