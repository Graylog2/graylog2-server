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
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled, { css } from 'styled-components';
import naturalSort from 'javascript-natural-sort';

import { LinkContainer, Link } from 'components/graylog/router';
import { Alert, Button } from 'components/graylog';
import { DataTable, Spinner } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import PipelineConnectionsList from './PipelineConnectionsList';

const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');
const { PipelineConnectionsStore, PipelineConnectionsActions } = CombinedProvider.get('PipelineConnections');
const { StreamsStore } = CombinedProvider.get('Streams');

const StyledAlert = styled(Alert)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px;
`;

const PipelineStage = styled.div(({ idle, theme }) => css`
  border: 1px solid ${theme.colors.gray[idle ? '50px' : '70px']};
  border-radius: 4px;
  display: inline-block;
  margin-right: 15px;
  padding: 20px;
  text-align: center;
  width: 120px;
  background-color: ${idle ? theme.utils.colorLevel(theme.colors.global.contentBackground, 10) : theme.colors.global.contentBackground};
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

const ProcessingTimelineComponent = createReactClass({
  displayName: 'ProcessingTimelineComponent',
  mixins: [Reflux.connect(PipelinesStore), Reflux.connect(PipelineConnectionsStore)],

  componentDidMount() {
    PipelinesActions.list();
    PipelineConnectionsActions.list();

    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams });
    });
  },

  _calculateUsedStages(pipelines) {
    return pipelines
      .map((pipeline) => pipeline.stages)
      .reduce((usedStages, pipelineStages) => {
        // Concat stages in a single array removing duplicates
        return usedStages.concat(pipelineStages.map((stage) => stage.stage).filter((stage) => usedStages.indexOf(stage) === -1));
      }, [])
      .sort(naturalSort);
  },

  _headerCellFormatter(header) {
    let className;

    if (header === 'Actions') {
      className = 'actions';
    }

    return <th className={className}>{header}</th>;
  },

  _formatConnectedStreams(streams) {
    return streams.map((s) => s.title).join(', ');
  },

  _formatStages(pipeline, stages) {
    const formattedStages = [];
    const stageNumbers = stages.map((stage) => stage.stage);

    this.usedStages.forEach((usedStage) => {
      if (stageNumbers.indexOf(usedStage) === -1) {
        formattedStages.push(
          <PipelineStage key={`${pipeline.id}-stage${usedStage}`} idle>Idle</PipelineStage>,
        );
      } else {
        formattedStages.push(
          <PipelineStage key={`${pipeline.id}-stage${usedStage}`}>Stage {usedStage}</PipelineStage>,
        );
      }
    }, this);

    return formattedStages;
  },

  _pipelineFormatter(pipeline) {
    const { connections, streams } = this.state;

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
                                   streamsFormatter={this._formatConnectedStreams}
                                   noConnectionsMessage={<em>Not connected</em>} />
        </StreamListTD>
        <td>{this._formatStages(pipeline, pipeline.stages)}</td>
        <td>
          <Button bsStyle="primary" bsSize="xsmall" onClick={this._deletePipeline(pipeline)}>Delete</Button>
          &nbsp;
          <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
            <Button bsStyle="info" bsSize="xsmall">Edit</Button>
          </LinkContainer>
        </td>
      </tr>
    );
  },

  _deletePipeline(pipeline) {
    return () => {
      // TODO: Replace with ConfirmDialog components
      // eslint-disable-next-line no-alert, no-restricted-globals
      if (confirm(`Do you really want to delete pipeline "${pipeline.title}"? This action cannot be undone.`)) {
        PipelinesActions.delete(pipeline.id);
      }
    };
  },

  _isLoading() {
    const { connections, pipelines, streams } = this.state;

    return !pipelines || !streams || !connections;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { pipelines } = this.state;
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

    this.usedStages = this._calculateUsedStages(pipelines);

    const headers = ['Pipeline', 'Connected to Streams', 'Processing Timeline', 'Actions'];

    return (
      <div>
        {addNewPipelineButton}
        <DataTable id="processing-timeline"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey="title"
                   rows={pipelines}
                   dataRowFormatter={this._pipelineFormatter}
                   filterLabel="Filter pipelines"
                   filterKeys={['title']} />
      </div>
    );
  },
});

export default ProcessingTimelineComponent;
