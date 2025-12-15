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
import { useEffect, useRef, useCallback } from 'react';
import styled from 'styled-components';

import type { InputSummary } from 'hooks/usePaginatedInputs';
import { useStore } from 'stores/connect';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { Spinner } from 'components/common';
import { formatCount, getValueFromMetric, prefixMetric } from 'components/inputs/helpers/InputThroughputUtils';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

type Props = {
  input: InputSummary;
};

const StyledSpan = styled.span`
  cursor: pointer;
`;

const METRIC_NAME = 'incomingMessages';

const ThroughputCell = ({ input }: Props) => {
  const metrics = useStore(MetricsStore, (store) => store.metrics);
  const metricName = prefixMetric(input, METRIC_NAME);
  const spanRef = useRef();
  const { toggleSection, expandedSections } = useExpandedSections();

  const toggleTrafficSection = useCallback(() => toggleSection(input.id, 'traffic'), [input.id, toggleSection]);

  useEffect(() => {
    MetricsActions.addGlobal(metricName);

    return () => {
      MetricsActions.removeGlobal(metricName);
    };
  }, [metricName]);

  if (!metrics) {
    return <Spinner size="xs" />;
  }

  const incomingMessages =
    Object.keys(metrics).reduce((previous, nodeId) => {
      if (!metrics?.[nodeId]?.[metricName]) {
        return previous;
      }

      const value = getValueFromMetric(metrics?.[nodeId]?.[metricName]);

      if (value !== undefined) {
        return isNaN(previous) ? value : previous + value;
      }

      return previous;
    }, NaN) || 0;

  const throughputSectionIsOpen = expandedSections?.[input.id]?.includes('traffic');

  return (
    <StyledSpan
      ref={spanRef}
      title={`${throughputSectionIsOpen ? 'Hide' : 'Show'} metrics`}
      onClick={toggleTrafficSection}>
      {!isNaN(incomingMessages) ? formatCount(incomingMessages) : 0} msg/s
    </StyledSpan>
  );
};

export default ThroughputCell;
