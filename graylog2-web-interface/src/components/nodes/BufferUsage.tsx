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
import PropTypes from 'prop-types';
import * as React from 'react';
import { useEffect } from 'react';
import styled, { css } from 'styled-components';

import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';
import { useStore } from 'stores/connect';
import type { GaugeMetric } from 'stores/metrics/MetricsStore';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';

import { Button } from '../bootstrap';
import { ProgressBar, Spinner } from '../common';
import { LinkContainer } from '../common/router';

const NodeBufferUsage = styled.div(({ theme }) => css`
  margin-top: ${theme.spacings.sm};
  margin-bottom: ${theme.spacings.xs};
`);

const StyledProgressBar = styled(ProgressBar)(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

const _metricPrefix = (bufferType) => `org.graylog2.buffers.${bufferType}`;

const _metricFilter = (bufferType) => `org\\.graylog2\\.buffers\\.${bufferType}\\.|${bufferType}buffer`;

type Props = {
  nodeId: string,
  bufferType: string,
  title: string,
};

const BufferUsage = ({ nodeId, bufferType, title }: Props) => {
  useEffect(() => {
    const prefix = _metricPrefix(bufferType);
    const metricNames = [
      `${prefix}.usage`,
      `${prefix}.size`,
    ];

    metricNames.forEach((metricName) => MetricsActions.add(nodeId, metricName));

    return () => metricNames.forEach((metricName) => MetricsActions.remove(nodeId, metricName));
  }, [nodeId, bufferType]);

  const { metrics } = useStore(MetricsStore);

  // metrics for this node could be undefined
  if (!metrics?.[nodeId]) {
    return <Spinner />;
  }

  const prefix = _metricPrefix(bufferType);

  const usageMetric = metrics[nodeId][`${prefix}.usage`] as GaugeMetric;
  const usage = usageMetric ? usageMetric.metric.value : NaN;
  const sizeMetric = metrics[nodeId][`${prefix}.size`] as GaugeMetric;
  const size = sizeMetric ? sizeMetric.metric.value : NaN;
  // eslint-disable-next-line no-restricted-globals
  const usagePercentage = ((!isNaN(usage) && !isNaN(size)) ? usage / size : 0);
  const percentLabel = NumberUtils.formatPercentage(usagePercentage);

  return (
    <div>
      <LinkContainer to={Routes.filtered_metrics(nodeId, _metricFilter(bufferType))}>
        <Button bsSize="xsmall" className="pull-right">Metrics</Button>
      </LinkContainer>
      <h3>{title}</h3>
      <NodeBufferUsage>
        <StyledProgressBar bars={[{
          value: usagePercentage * 100,
          bsStyle: 'warning',
          label: percentLabel,
        }]} />
      </NodeBufferUsage>
      <span><strong>{usage} messages</strong> in {title.toLowerCase()}, {percentLabel} utilized.</span>
    </div>
  );
};

BufferUsage.propTypes = {
  bufferType: PropTypes.string.isRequired,
  nodeId: PropTypes.string.isRequired,
  title: PropTypes.node.isRequired,
};

export default BufferUsage;
