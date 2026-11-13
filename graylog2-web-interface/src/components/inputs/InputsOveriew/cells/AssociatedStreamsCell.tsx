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
import EntityTitlesCellList from 'components/common/EntityTitlesCellList';
import Routes from 'routing/Routes';
import { useInputMetricsFor } from 'components/inputs/InputsOveriew/InputMetricsContext';

type Props = {
  input: InputSummary;
};

const AssociatedStreamsCell = ({ input }: Props) => {
  const { metrics, isInitialLoading, isError } = useInputMetricsFor(input.id);

  if (isInitialLoading && !metrics) {
    return <Spinner size="xs" />;
  }

  const streamIds = Object.keys(metrics?.messages_per_stream ?? {});

  if (isError || !streamIds.length) {
    return <span aria-label={`No associated streams available for input ${input.title}`}>—</span>;
  }

  return <EntityTitlesCellList entityIds={streamIds} entityType="streams" entityRoute={Routes.stream_view} />;
};

export default AssociatedStreamsCell;
