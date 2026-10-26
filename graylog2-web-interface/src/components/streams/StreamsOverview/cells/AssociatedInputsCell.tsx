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
import { Spinner } from 'components/common';
import ExpandedSectionCount from 'components/common/EntityDataTable/ExpandedSectionCount';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import { METRIC_COLUMN_IDS } from 'components/streams/StreamsOverview/metricColumns';

type Props = {
  stream: Stream;
};

const SECTION_NAME = METRIC_COLUMN_IDS.associatedInputs;

const AssociatedInputsCell = ({ stream }: Props) => {
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  if (isError || !metrics?.associated_inputs?.length) {
    return null;
  }

  return (
    <ExpandedSectionCount
      entityId={stream.id}
      section={SECTION_NAME}
      count={metrics.associated_inputs.length}
      label={`associated inputs for ${stream.title}`}
    />
  );
};

export default AssociatedInputsCell;
