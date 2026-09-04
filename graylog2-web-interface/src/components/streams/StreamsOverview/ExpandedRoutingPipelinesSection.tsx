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
import useEntityTitles from 'hooks/useEntityTitles';
import { Link, Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import Routes from 'routing/Routes';

type Props = {
  stream: Stream;
};

const ExpandedRoutingPipelinesSection = ({ stream }: Props) => {
  const { metrics, isInitialLoading, isError } = useStreamMetricsFor(stream.id);
  const pipelineIds = metrics?.routing_pipelines;

  const titleEntities = (pipelineIds ?? []).map((id) => ({ id, type: 'pipeline_processor_pipelines' }));
  const { titlesById, isInitialLoading: areTitlesLoading } = useEntityTitles(titleEntities);

  if (isInitialLoading && !pipelineIds) {
    return <Spinner />;
  }

  if (isError) {
    return <p>Could not load routing pipelines.</p>;
  }

  if (!pipelineIds || pipelineIds.length === 0) {
    return <p>No routing pipelines route messages to this stream.</p>;
  }

  return (
    <ListGroup componentClass="ul">
      {pipelineIds.map((pipelineId) => {
        const title = titlesById[pipelineId];
        const label = title ?? (areTitlesLoading ? pipelineId : `${pipelineId} (deleted)`);

        return (
          <ListGroupItem key={pipelineId}>
            {title ? <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipelineId)}>{label}</Link> : label}
          </ListGroupItem>
        );
      })}
    </ListGroup>
  );
};

export default ExpandedRoutingPipelinesSection;
