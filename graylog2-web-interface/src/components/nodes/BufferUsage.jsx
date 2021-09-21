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
import styled from 'styled-components';

import { LinkContainer } from 'components/graylog/router';
import { Button, ProgressBar } from 'components/graylog';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';
import { Spinner } from 'components/common';
import { useStore } from 'stores/connect';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const NodeBufferUsage = styled.div`
  margin-top: 10px;
  margin-bottom: 7px;
`;

const StyledProgressBar = styled(ProgressBar)`
  margin-bottom: 5px;
`;

const _metricPrefix = (bufferType) => `org.graylog2.buffers.${bufferType}`;

const _metricFilter = (bufferType) => `org\\.graylog2\\.buffers\\.${bufferType}\\.|${bufferType}buffer`;

const BufferUsage = ({ nodeId, bufferType, title }) => {
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

  if (!metrics) {
    return <Spinner />;
  }

  const prefix = _metricPrefix(bufferType);

  const usageMetric = metrics[nodeId][`${prefix}.usage`];
  const usage = usageMetric ? usageMetric.metric.value : NaN;
  const sizeMetric = metrics[nodeId][`${prefix}.size`];
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
