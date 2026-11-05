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
import numeral from 'numeral';
import { Trans, useTranslation } from 'react-i18next';

import { Spinner } from 'components/common';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { useNodeMetrics } from 'hooks/useMetrics';

type Props = {
  nodeId: string;
  longFormat?: boolean;
};
const metricNames = {
  totalIn: 'org.graylog2.throughput.input.1-sec-rate',
  totalOut: 'org.graylog2.throughput.output.1-sec-rate',
};

const metricValues = Object.values(metricNames);

const NodeThroughput = ({ nodeId, longFormat = false }: Props) => {
  const { t } = useTranslation('system');
  const { data: nodeMetrics, isLoading } = useNodeMetrics(nodeId, metricValues);

  if (isLoading || !nodeMetrics) {
    return <Spinner text={t('node_throughput.loading')} />;
  }

  const metrics = MetricsExtractor.getValuesForNode(nodeMetrics, metricNames);

  if (Object.keys(metrics).length === 0) {
    return <span>{t('node_throughput.unable_to_load')}</span>;
  }

  const totalIn = numeral(metrics.totalIn).format('0,0');
  const totalOut = numeral(metrics.totalOut).format('0,0');

  if (longFormat) {
    return (
      <span>
        <Trans
          t={t}
          i18nKey="node_throughput.processing_long"
          values={{ totalIn, totalOut }}
          components={{ strong: <strong /> }}
        />
      </span>
    );
  }

  return <span>{t('node_throughput.short', { totalIn, totalOut })}</span>;
};

export default NodeThroughput;
