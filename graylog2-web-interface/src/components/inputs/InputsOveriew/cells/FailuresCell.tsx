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
import styled, { css } from 'styled-components';

import type { InputSummary } from 'hooks/usePaginatedInputs';
import { useMetrics } from 'hooks/useMetrics';
import { Link, Spinner } from 'components/common';
import { formatCount, getValueFromMetric } from 'components/inputs/helpers/InputThroughputUtils';
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
  const metricNames = FAILURE_SUFFIXES.map((suffix) => buildMetricName(input.id, suffix));
  const { data: metrics, isLoading } = useMetrics(metricNames);

  if (isLoading) {
    return <Spinner size="xs" />;
  }

  const totalFailures = metricNames.reduce((sum, metricName) => {
    const aggregated = Object.keys(metrics).reduce(
      (prev, nodeId) => prev + getValueFromMetric(metrics[nodeId]?.[metricName]),
      0,
    );

    return sum + aggregated;
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
