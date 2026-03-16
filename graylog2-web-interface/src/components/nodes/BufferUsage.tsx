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
import { useMemo } from 'react';
import styled, { css } from 'styled-components';

import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';
import type { GaugeMetric } from 'types/metrics';
import { useNodeMetrics } from 'hooks/useMetrics';

import { Button } from '../bootstrap';
import { LinkContainer, ProgressBar, Spinner } from '../common';

const NodeBufferUsage = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.sm};
    margin-bottom: ${theme.spacings.xs};
  `,
);

const StyledProgressBar = styled(ProgressBar)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xs};
  `,
);

const _metricPrefix = (bufferType) => `org.graylog2.buffers.${bufferType}`;

const _metricFilter = (bufferType) => `org\\.graylog2\\.buffers\\.${bufferType}\\.|${bufferType}buffer`;

type Props = {
  nodeId: string;
  bufferType: string;
  title: string;
};

const BufferUsage = ({ nodeId, bufferType, title }: Props) => {
  const prefix = _metricPrefix(bufferType);
  const metricNames = useMemo(() => [`${prefix}.usage`, `${prefix}.size`], [prefix]);
  const { data: nodeMetrics } = useNodeMetrics(nodeId, metricNames);

  if (!nodeMetrics) {
    return <Spinner />;
  }

  const usageMetric = nodeMetrics[`${prefix}.usage`] as GaugeMetric;
  const usage = usageMetric ? usageMetric.metric.value : NaN;
  const sizeMetric = nodeMetrics[`${prefix}.size`] as GaugeMetric;
  const size = sizeMetric ? sizeMetric.metric.value : NaN;

  const usagePercentage = !isNaN(usage) && !isNaN(size) ? usage / size : 0;
  const percentLabel = NumberUtils.formatPercentage(usagePercentage);

  return (
    <div>
      <LinkContainer to={Routes.filtered_metrics(nodeId, _metricFilter(bufferType))}>
        <Button bsSize="xsmall" className="pull-right">
          Metrics
        </Button>
      </LinkContainer>
      <h3>{title}</h3>
      <NodeBufferUsage>
        <StyledProgressBar
          bars={[
            {
              value: usagePercentage * 100,
              bsStyle: 'warning',
              label: percentLabel,
            },
          ]}
        />
      </NodeBufferUsage>
      <span>
        <strong>{usage} messages</strong> in {title.toLowerCase()}, {percentLabel} utilized.
      </span>
    </div>
  );
};

export default BufferUsage;
