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
import { useTranslation } from 'react-i18next';

import type { Stream } from 'logic/streams/types';
import { Spinner } from 'components/common';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import ProcessingTimeBadge from 'components/streams/StreamsOverview/cells/ProcessingTimeBadge';

type Props = {
  stream: Stream;
};

const AvgProcessingTimeCell = ({ stream }: Props) => {
  const { t } = useTranslation('streams');
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  if (isError || typeof metrics?.avg_processing_time_ms !== 'number') {
    return null;
  }

  return (
    <ProcessingTimeBadge
      ms={metrics.avg_processing_time_ms}
      title={t('streams_overview.cells.avg_processing_time', { ms: metrics.avg_processing_time_ms.toFixed(2) })}
    />
  );
};

export default AvgProcessingTimeCell;
