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

import { LinkToNode } from 'components/common';
import {
  calculateInputMetricsByNode,
  getInputConnectionMetrics,
  formatCount,
} from 'components/inputs/helpers/InputThroughputUtils';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import { Connections, NetworkIOStats } from 'components/inputs/InputsOveriew';

const NodeMetricsDetails = ({ input, metrics }: { metrics: Record<string, any>; input: InputSummary }) => {
  const calculatedMetricsByNode = calculateInputMetricsByNode(input, metrics);

  return (
    <span>
      <hr />
      {Object.keys(calculatedMetricsByNode).map((nodeId) => {
        const {
          openConnections,
          totalConnections,
          emptyMessages,
          writtenBytes1Sec,
          writtenBytesTotal,
          readBytes1Sec,
          readBytesTotal,
        } = getInputConnectionMetrics(input, calculatedMetricsByNode[nodeId] || {});

        return (
          <span key={input.id + nodeId}>
            <LinkToNode nodeId={nodeId} />
            <br />
            {!isNaN(writtenBytes1Sec) && (
              <NetworkIOStats
                writtenBytes1Sec={writtenBytes1Sec}
                writtenBytesTotal={writtenBytesTotal}
                readBytes1Sec={readBytes1Sec}
                readBytesTotal={readBytesTotal}
              />
            )}
            <br />
            {!isNaN(openConnections) && (
              <Connections openConnections={openConnections} totalConnections={totalConnections} />
            )}
            {!isNaN(emptyMessages) && (
              <span>
                Empty messages discarded: {formatCount(emptyMessages)}
                <br />
              </span>
            )}
            {isNaN(writtenBytes1Sec) && isNaN(openConnections) && <span>No metrics available for this node</span>}
          </span>
        );
      })}
    </span>
  );
};

export default NodeMetricsDetails;
