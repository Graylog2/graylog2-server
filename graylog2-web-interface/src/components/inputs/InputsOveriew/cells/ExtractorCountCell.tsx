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

const SECTION_NAME = METRIC_COLUMN_IDS.extractorCount;

const ExtractorCountCell = ({ input }: Props) => {
  const { metrics, isInitialLoading, isError } = useInputMetricsFor(input.id);
  const { toggleSection, expandedSections } = useExpandedSections();

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  if (isError || metrics?.extractor_count === undefined) {
    return <span aria-label={`No extractor count available for input ${input.title}`}>—</span>;
  }

  const isOpen = expandedSections?.[input.id]?.includes(SECTION_NAME) ?? false;
  const title = `${isOpen ? 'Hide' : 'Show'} extractors for ${input.title}`;

  return (
    <CountBadge
      count={metrics.extractor_count}
      iconName={isOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
      onClick={() => toggleSection(input.id, SECTION_NAME)}
      title={title}
    />
  );
};

export default ExtractorCountCell;
