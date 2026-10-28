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
import useStreamMetrics from 'hooks/useStreamMetrics';
import type { StreamMetricField, StreamMetrics } from 'hooks/useStreamMetrics';

// Loads the metrics of one stream, e.g. for an expanded section, independent of the metrics requested for the visible columns.
const useSingleStreamMetrics = (
  streamId: string,
  fields: Array<StreamMetricField>,
): {
  metrics: StreamMetrics | undefined;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const { metricsByStreamId, isInitialLoading, isError } = useStreamMetrics([streamId], fields);

  return { metrics: metricsByStreamId[streamId], isInitialLoading, isError };
};

export default useSingleStreamMetrics;
