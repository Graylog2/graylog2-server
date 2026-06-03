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
import { CountBadge, Spinner } from 'components/common';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { useInputMetricsFor } from 'components/inputs/InputsOveriew/InputMetricsContext';
import { METRIC_COLUMN_IDS } from 'components/inputs/InputsOveriew/metricColumns';

type Props = {
  input: InputSummary;
};

const SECTION_NAME = METRIC_COLUMN_IDS.messagesPerStream;

const sumValues = (counts: Record<string, number>) => Object.values(counts).reduce((sum, value) => sum + value, 0);

const MessageCountCell = ({ input }: Props) => {
  const { metrics, isInitialLoading, isError } = useInputMetricsFor(input.id);
  const { toggleSection, expandedSections } = useExpandedSections();

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  if (isError || !metrics?.messages_per_stream) {
    return <span aria-label={`No message count available for input ${input.title}`}>—</span>;
  }

  const total = sumValues(metrics.messages_per_stream);
  const isOpen = expandedSections?.[input.id]?.includes(SECTION_NAME) ?? false;
  const title = `${isOpen ? 'Hide' : 'Show'} messages per stream for ${input.title}`;

  return (
    <CountBadge
      count={total}
      iconName={isOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
      onClick={() => toggleSection(input.id, SECTION_NAME)}
      title={title}
    />
  );
};

export default MessageCountCell;
