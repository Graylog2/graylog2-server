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
import { useEffect, useState } from 'react';

import { Spinner } from 'components/common';
import { Button } from 'components/bootstrap';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';
import {
  getMetricNamesForInput,
  calculateInputMetrics,
  getInputConnectionMetrics,
  formatCount,
} from 'components/inputs/helpers/InputThroughputUtils';
import { NetworkIOStats, Connections, NodesMetricsDetails } from 'components/inputs/InputsOveriew';

type Props = {
  input: InputSummary;
};

const ThroughputSection = ({ input }: Props) => {
  const metrics = useStore(MetricsStore, (store) => store.metrics);
  const metricNames = getMetricNamesForInput(input);
  const [showDetails, setShowDetails] = useState(false);

  const toggleShowDetails = () => {
    setShowDetails(!showDetails);
  };

  useEffect(() => {
    metricNames.map((metricName) => MetricsActions.addGlobal(metricName));

    return () => {
      metricNames.map((metricName) => MetricsActions.removeGlobal(metricName));
    };
  }, [input, metricNames]);

  if (!metrics) {
    return <Spinner />;
  }

  const calculatedMetrics = calculateInputMetrics(input, metrics);

  const {
    openConnections,
    totalConnections,
    emptyMessages,
    writtenBytes1Sec,
    writtenBytesTotal,
    readBytes1Sec,
    readBytesTotal,
  } = getInputConnectionMetrics(input, calculatedMetrics);

  return (
    <span>
      {isNaN(writtenBytes1Sec) && isNaN(openConnections) && <i>No metrics available for this input</i>}
      {!isNaN(writtenBytes1Sec) && (
        <NetworkIOStats
          readBytes1Sec={readBytes1Sec}
          writtenBytes1Sec={writtenBytes1Sec}
          readBytesTotal={readBytesTotal}
          writtenBytesTotal={writtenBytesTotal}
        />
      )}
      <br />
      {!isNaN(openConnections) && !isNaN(totalConnections) && (
        <Connections openConnections={openConnections} totalConnections={totalConnections} />
      )}
      {!isNaN(emptyMessages) && (
        <span>
          Empty messages discarded: {formatCount(emptyMessages)}
          <br />
        </span>
      )}
      {!isNaN(writtenBytes1Sec) && input.global && (
        <Button bsStyle="link" onClick={toggleShowDetails}>
          {showDetails ? 'Hide' : 'Show'} details
        </Button>
      )}
      <br />
      {!isNaN(writtenBytes1Sec) && showDetails && <NodesMetricsDetails input={input} metrics={metrics} />}
    </span>
  );
};

export default ThroughputSection;
