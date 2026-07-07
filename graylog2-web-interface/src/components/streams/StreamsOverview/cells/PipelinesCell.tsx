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

import type { Stream } from 'logic/streams/types';
import { CountBadge, Spinner } from 'components/common';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import { METRIC_COLUMN_IDS } from 'components/streams/StreamsOverview/metricColumns';

type Props = {
  stream: Stream;
};

const SECTION_NAME = METRIC_COLUMN_IDS.pipelines;

const PipelinesCell = ({ stream }: Props) => {
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);
  const { toggleSection, expandedSections } = useExpandedSections();

  if (stream.is_default || !stream.is_editable) {
    return null;
  }

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  if (isError || !metrics?.pipelines?.length) {
    return null;
  }

  const count = metrics.pipelines.length;

  const isOpen = expandedSections?.[stream.id]?.includes(SECTION_NAME) ?? false;
  const title = `${isOpen ? 'Hide' : 'Show'} connected pipelines for ${stream.title}`;

  return (
    <CountBadge
      count={count}
      iconName={isOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
      onClick={() => toggleSection(stream.id, SECTION_NAME)}
      title={title}
    />
  );
};

export default PipelinesCell;
