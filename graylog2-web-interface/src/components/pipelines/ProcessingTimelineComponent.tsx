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
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import naturalSort from 'javascript-natural-sort';

import { LinkContainer, Link } from 'components/graylog/router';
import { Alert, Button } from 'components/graylog';
import { DataTable, Spinner } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { useStore } from 'stores/connect';
import StreamsStore, { Stream } from 'stores/streams/StreamsStore';

import PipelineConnectionsList from './PipelineConnectionsList';

const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');
const { PipelineConnectionsStore, PipelineConnectionsActions } = CombinedProvider.get('PipelineConnections');

const StyledAlert = styled(Alert)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px;
`;

const PipelineStage = styled.div<{ $idle?: boolean }>(({ $idle, theme }) => css`
  border: 1px solid ${theme.colors.gray[$idle ? '50px' : '70px']};
  border-radius: 4px;
  display: inline-block;
  margin-right: 15px;
  padding: 20px;
  text-align: center;
  width: 120px;
  background-color: ${$idle ? theme.utils.colorLevel(theme.colors.global.contentBackground, 10) : theme.colors.global.contentBackground};
`);

const PipelineNameTD = styled.td`
  max-width: 300px;
  overflow-x: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 300px;
`;

const StreamListTD = styled.td`
  max-width: 150px;
  width: 150px;
  word-wrap: break-word;
`;

const _formatConnectedStreams = (streams) => {
  return streams.map((s) => s.title).join(', ');
};

const ProcessingTimelineComponent = () => {
  const { pipelines } = useStore(PipelinesStore);
  const { connections } = useStore(PipelineConnectionsStore);
  const [streams, setStreams] = useState<Stream[] | undefined>();

  useEffect(() => {
    PipelinesActions.list();
    PipelineConnectionsActions.list();
    StreamsStore.listStreams().then(setStreams);
  }, []);

  const isLoading = !pipelines || !streams || !connections;

  if (isLoading) {
    return <Spinner />;
  }

  const _headerCellFormatter = (header) => {
    let className;

    if (header === 'Actions') {
      className = 'actions';
    }

    return <th className={className}>{header}</th>;
  };

  const _formatStages = (pipeline, stages) => {
    const stageNumbers = stages.map((stage) => stage.stage);

    return pipelines
      .map((p) => p.stages.map(({ stage }) => stage))
      .reduce((usedStagesAcc: number[], pipelineStages: number[]) => {
        // Concat stages in a single array removing duplicates
        return Array.from(new Set([...usedStagesAcc, ...pipelineStages]));
      }, [])
      .sort(naturalSort)
      .map((usedStage) => {
        if (stageNumbers.indexOf(usedStage) === -1) {
          return <PipelineStage key={`${pipeline.id}-stage${usedStage}`} $idle>Idle</PipelineStage>;
        }

        return <PipelineStage key={`${pipeline.id}-stage${usedStage}`}>Stage {usedStage}</PipelineStage>;
      });
  };

  const _deletePipeline = (pipeline) => {
    return () => {
      // TODO: Replace with ConfirmDialog components
      if (window.confirm(`Do you really want to delete pipeline "${pipeline.title}"? This action cannot be undone.`)) {
        PipelinesActions.delete(pipeline.id);
      }
    };
  };

  const _pipelineFormatter = (pipeline) => {
    return (
      <tr key={pipeline.id}>
        <PipelineNameTD>
          <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)} title={pipeline.title}>{pipeline.title}</Link>
          <br />
          {pipeline.description}
          <br />
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${pipeline.id}.executed`}>
            <CounterRate prefix="Throughput:" suffix="msg/s" />
          </MetricContainer>
        </PipelineNameTD>
        <StreamListTD>
          <PipelineConnectionsList pipeline={pipeline}
                                   connections={connections}
                                   streams={streams}
                                   streamsFormatter={_formatConnectedStreams}
                                   noConnectionsMessage={<em>Not connected</em>} />
        </StreamListTD>
        <td>{_formatStages(pipeline, pipeline.stages)}</td>
        <td>
          <Button bsStyle="primary" bsSize="xsmall" onClick={_deletePipeline(pipeline)}>Delete</Button>
          &nbsp;
          <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
            <Button bsStyle="info" bsSize="xsmall">Edit</Button>
          </LinkContainer>
        </td>
      </tr>
    );
  };

  const addNewPipelineButton = (
    <div className="pull-right">
      <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE('new')}>
        <Button bsStyle="success">Add new pipeline</Button>
      </LinkContainer>
    </div>
  );

  if (pipelines.length === 0) {
    return (
      <div>
        <StyledAlert>
          <span>There are no pipelines configured in your system. Create one to start processing your messages.</span>
          {addNewPipelineButton}
        </StyledAlert>
      </div>
    );
  }

  const headers = ['Pipeline', 'Connected to Streams', 'Processing Timeline', 'Actions'];

  return (
    <div>
      {addNewPipelineButton}
      <DataTable id="processing-timeline"
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={_headerCellFormatter}
                 sortByKey="title"
                 rows={pipelines}
                 dataRowFormatter={_pipelineFormatter}
                 filterLabel="Filter pipelines"
                 filterKeys={['title']} />
    </div>
  );
};

ProcessingTimelineComponent.propTypes = {};

export default ProcessingTimelineComponent;
