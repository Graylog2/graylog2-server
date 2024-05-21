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
import { useParams } from 'react-router-dom';

import SectionComponent from 'components/common/Section/SectionComponent';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';
import { Table } from 'components/bootstrap';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

const StreamDataRoutingProcessing = () => {
  const { streamId } = useParams<{streamId: string}>();
  const { data: connectedPipelines, isInitialLoading } = usePipelinesConnectedStream(streamId);
  const hasConnectedPipelines = !isInitialLoading && connectedPipelines?.length;

  return (
    <SectionComponent title="Data Routing - Processing">
      <Table condensed>
        <thead>
          <tr>
            <th>Pipeline</th>
          </tr>
        </thead>
        <tbody>
          {hasConnectedPipelines && connectedPipelines.map((pipeline) => (
            <tr key={pipeline.id}>
              <td>
                <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)} target="_blank">{pipeline.title}</Link>
              </td>
            </tr>
          ))}
          {!hasConnectedPipelines && (
          <tr>
            <td colSpan={2}>This stream is not connected to any Pipeline.</td>
          </tr>
          )}
        </tbody>
      </Table>
    </SectionComponent>
  );
};

export default StreamDataRoutingProcessing;
