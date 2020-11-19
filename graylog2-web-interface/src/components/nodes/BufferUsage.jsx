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
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled from 'styled-components';

import { LinkContainer } from 'components/graylog/router';
import { Button, ProgressBar } from 'components/graylog';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';
import { Spinner } from 'components/common';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const NodeBufferUsage = styled.div`
  margin-top: 10px;
  margin-bottom: 7px;
`;

const StyledProgressBar = styled(ProgressBar)`
  margin-bottom: 5px;
`;

const BufferUsage = createReactClass({
  displayName: 'BufferUsage',

  propTypes: {
    bufferType: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    title: PropTypes.node.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillMount() {
    const { nodeId } = this.props;
    const prefix = this._metricPrefix();
    const metricNames = [
      `${prefix}.usage`,
      `${prefix}.size`,
    ];

    metricNames.forEach((metricName) => MetricsActions.add(nodeId, metricName));
  },

  _metricPrefix() {
    const { bufferType } = this.props;

    return `org.graylog2.buffers.${bufferType}`;
  },

  _metricFilter() {
    const { bufferType } = this.props;

    return `org\\.graylog2\\.buffers\\.${bufferType}\\.|${bufferType}buffer`;
  },

  render() {
    const { metrics } = this.state;

    if (!metrics) {
      return <Spinner />;
    }

    const { nodeId, title } = this.props;
    const prefix = this._metricPrefix();
    const usageMetric = metrics[nodeId][`${prefix}.usage`];
    const usage = usageMetric ? usageMetric.metric.value : NaN;
    const sizeMetric = metrics[nodeId][`${prefix}.size`];
    const size = sizeMetric ? sizeMetric.metric.value : NaN;
    // eslint-disable-next-line no-restricted-globals
    const usagePercentage = ((!isNaN(usage) && !isNaN(size)) ? usage / size : 0);
    const percentLabel = NumberUtils.formatPercentage(usagePercentage);

    return (
      <div>
        <LinkContainer to={Routes.filtered_metrics(nodeId, this._metricFilter())}>
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
  },
});

export default BufferUsage;
