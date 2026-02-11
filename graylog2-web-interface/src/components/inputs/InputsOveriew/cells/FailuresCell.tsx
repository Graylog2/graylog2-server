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
import { useEffect, useMemo } from 'react';
import styled, { css } from 'styled-components';

import type { InputSummary } from 'hooks/usePaginatedInputs';
import { useStore } from 'stores/connect';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { Spinner } from 'components/common';
import { formatCount, getValueFromMetric } from 'components/inputs/helpers/InputThroughputUtils';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

type Props = {
  input: InputSummary;
};

const FAILURE_SUFFIXES = ['failures.input', 'failures.processing', 'failures.indexing'] as const;

const buildMetricName = (inputId: string, suffix: string) => `org.graylog2.inputs.${inputId}.${suffix}`;

const StyledLink = styled(Link)<{ $hasFailures: boolean }>(
  ({ theme, $hasFailures }) => css`
    color: ${$hasFailures ? theme.colors.variant.danger : 'inherit'};
    font-weight: ${$hasFailures ? 'bold' : 'normal'};
  `,
);

const FailuresCell = ({ input }: Props) => {
  const metrics = useStore(MetricsStore, (store) => store.metrics);

  const metricNames = useMemo(
    () => FAILURE_SUFFIXES.map((suffix) => buildMetricName(input.id, suffix)),
    [input.id],
  );

  useEffect(() => {
    metricNames.forEach((name) => MetricsActions.addGlobal(name));

    return () => {
      metricNames.forEach((name) => MetricsActions.removeGlobal(name));
    };
  }, [metricNames]);

  if (!metrics) {
    return <Spinner size="xs" />;
  }

  const totalFailures = metricNames.reduce((sum, metricName) => {
    const aggregated = Object.keys(metrics).reduce((prev, nodeId) => {
      if (!metrics?.[nodeId]?.[metricName]) {
        return prev;
      }

      const value = getValueFromMetric(metrics[nodeId][metricName]);

      if (value !== undefined) {
        return isNaN(prev) ? value : prev + value;
      }

      return prev;
    }, NaN);

    const safeAggregated = isNaN(aggregated) ? 0 : aggregated;

    return sum + safeAggregated;
  }, 0);

  return (
    <StyledLink
      to={Routes.SYSTEM.INPUT_DIAGNOSIS(input.id)}
      title={`Show input diagnosis for ${input.title}`}
      $hasFailures={totalFailures > 0}>
      {formatCount(totalFailures)}
    </StyledLink>
  );
};

export default FailuresCell;
