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

import type { InputSummary } from 'hooks/usePaginatedInputs';
import { Spinner } from 'components/common';
import ExpandedSectionCount from 'components/common/EntityDataTable/ExpandedSectionCount';
import { useInputMetricsFor } from 'components/inputs/InputsOveriew/InputMetricsContext';
import { METRIC_COLUMN_IDS } from 'components/inputs/InputsOveriew/metricColumns';

type Props = {
  input: InputSummary;
};

const SECTION_NAME = METRIC_COLUMN_IDS.messagesPerStream;

const sumValues = (counts: Record<string, number>) => Object.values(counts).reduce((sum, value) => sum + value, 0);

const MessageCountCell = ({ input }: Props) => {
  const { metrics, isInitialLoading, isError } = useInputMetricsFor(input.id);

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  if (isError || !metrics?.messages_per_stream) {
    return <span aria-label={`No message count available for input ${input.title}`}>—</span>;
  }

  const total = sumValues(metrics.messages_per_stream);

  return (
    <ExpandedSectionCount
      entityId={input.id}
      section={SECTION_NAME}
      count={total}
      label={`messages per stream for ${input.title}`}
      abbreviate
    />
  );
};

export default MessageCountCell;
