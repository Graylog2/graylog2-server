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
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import { formatCount } from 'components/inputs/helpers/InputThroughputUtils';

type Props = {
  stream: Stream;
};

const MessageCountCell = ({ stream }: Props) => {
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  if (isError || !metrics?.message_count) {
    return null;
  }

  return (
    <span title={`${metrics.message_count} messages in the last 24 hours`}>{formatCount(metrics.message_count)}</span>
  );
};

export default MessageCountCell;
