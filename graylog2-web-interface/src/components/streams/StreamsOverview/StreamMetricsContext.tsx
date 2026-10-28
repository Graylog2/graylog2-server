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
import { createContext, useContext } from 'react';

import useStreamMetrics from 'hooks/useStreamMetrics';
import type { StreamMetricField, StreamMetrics, StreamMetricsByStreamId } from 'hooks/useStreamMetrics';
import { singleton } from 'logic/singleton';

type StreamMetricsContextValue = {
  metricsByStreamId: StreamMetricsByStreamId;
  isInitialLoading: boolean;
  isError: boolean;
  isFieldRequested: (field: StreamMetricField | string) => boolean;
};

const EMPTY_CONTEXT: StreamMetricsContextValue = {
  metricsByStreamId: {},
  isInitialLoading: false,
  isError: false,
  isFieldRequested: () => false,
};

const StreamMetricsContext = singleton('contexts.StreamMetricsContext', () =>
  createContext<StreamMetricsContextValue>(EMPTY_CONTEXT),
);

type Props = React.PropsWithChildren<{
  streamIds: Array<string>;
  fields: Array<StreamMetricField | string>;
}>;

export const StreamMetricsProvider = ({ streamIds, fields, children = undefined }: Props) => {
  const { metricsByStreamId, isInitialLoading, isError } = useStreamMetrics(streamIds, fields);
  const requestedFields = new Set(fields);

  const value: StreamMetricsContextValue = {
    metricsByStreamId,
    isInitialLoading,
    isError,
    isFieldRequested: (field) => requestedFields.has(field),
  };

  return <StreamMetricsContext.Provider value={value}>{children}</StreamMetricsContext.Provider>;
};

export const useStreamMetricsContext = (): StreamMetricsContextValue => useContext(StreamMetricsContext);

// Loads the `fields` for this stream only when the provider didn't request them, e.g. because their column is hidden.
export const useStreamMetricsFor = (
  streamId: string,
  fields: Array<StreamMetricField | string> = [],
): {
  metrics: StreamMetrics | undefined;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const { metricsByStreamId, isInitialLoading, isError, isFieldRequested } = useContext(StreamMetricsContext);
  const isProvided = fields.every(isFieldRequested);
  const ownMetrics = useStreamMetrics([streamId], isProvided ? [] : fields);

  if (!isProvided) {
    return {
      metrics: ownMetrics.metricsByStreamId[streamId],
      isInitialLoading: ownMetrics.isInitialLoading,
      isError: ownMetrics.isError,
    };
  }

  return {
    metrics: metricsByStreamId[streamId],
    isInitialLoading,
    isError,
  };
};

export default StreamMetricsContext;
