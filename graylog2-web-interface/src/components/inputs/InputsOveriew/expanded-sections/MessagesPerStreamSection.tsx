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
import { useMemo } from 'react';

import type { InputSummary } from 'hooks/usePaginatedInputs';
import useEntityTitles from 'hooks/useEntityTitles';
import { Link, Spinner } from 'components/common';
import { Table } from 'components/bootstrap';
import { formatCount } from 'components/inputs/helpers/InputThroughputUtils';
import { useInputMetricsFor } from 'components/inputs/InputsOveriew/InputMetricsContext';
import Routes from 'routing/Routes';

type Props = {
  input: InputSummary;
};

const MessagesPerStreamSection = ({ input }: Props) => {
  const { metrics, isInitialLoading, isError } = useInputMetricsFor(input.id);
  const messagesPerStream = metrics?.messages_per_stream;

  const streamIds = useMemo(() => (messagesPerStream ? Object.keys(messagesPerStream) : []), [messagesPerStream]);
  const titleEntities = useMemo(() => streamIds.map((id) => ({ id, type: 'streams' })), [streamIds]);
  const { titlesById, isInitialLoading: areTitlesLoading } = useEntityTitles(titleEntities);

  if (isInitialLoading && !messagesPerStream) {
    return <Spinner />;
  }

  if (isError) {
    return <p>Could not load messages per stream.</p>;
  }

  if (!messagesPerStream || streamIds.length === 0) {
    return <p>No streams have received messages from this input in the last 24 hours.</p>;
  }

  const sortedEntries = Object.entries(messagesPerStream).sort(([, a], [, b]) => b - a);

  return (
    <Table bordered condensed striped hover>
      <thead>
        <tr>
          <th>Stream</th>
          <th>Messages</th>
        </tr>
      </thead>
      <tbody>
        {sortedEntries.map(([streamId, count]) => {
          const title = titlesById[streamId];
          const label = title ?? (areTitlesLoading ? streamId : `${streamId} (deleted)`);

          return (
            <tr key={streamId}>
              <td>{title ? <Link to={Routes.stream_view(streamId)}>{label}</Link> : label}</td>
              <td>{formatCount(count)}</td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
};

export default MessagesPerStreamSection;
