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
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';

import { Row, Col, Alert } from 'components/graylog';
import { EntityList, Pluralize } from 'components/common';

import Stage from './Stage';
import StageForm from './StageForm';
import PipelineDetails from './PipelineDetails';
import PipelineConnectionsForm from './PipelineConnectionsForm';
import PipelineConnectionsList from './PipelineConnectionsList';

const Pipeline = createReactClass({
  displayName: 'Pipeline',

  propTypes: {
    pipeline: PropTypes.object.isRequired,
    connections: PropTypes.array.isRequired,
    streams: PropTypes.array.isRequired,
    onConnectionsChange: PropTypes.func.isRequired,
    onStagesChange: PropTypes.func.isRequired,
    onPipelineChange: PropTypes.func.isRequired,
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!./Pipeline.css'),

  _connections_warning() {
    if (this.props.connections.length == 0) {
      return (
        <Alert bsStyle="danger" className="pipeline-no-connections-warning">
          This pipeline is currently not connected to any streams. You have to connect a pipeline to at least one
          stream to make it process incoming messages. Note that this is not required if you intend to use this
          pipeline only for search result transformation using decorators.
        </Alert>
      );
    }
  },

  _saveStage(stage, callback) {
    const newStages = this.props.pipeline.stages.slice();

    newStages.push(stage);
    this.props.onStagesChange(newStages, callback);
  },

  _updateStage(prevStage) {
    return (stage, callback) => {
      const newStages = this.props.pipeline.stages.filter((s) => s.stage !== prevStage.stage);

      newStages.push(stage);
      this.props.onStagesChange(newStages, callback);
    };
  },

  _deleteStage(stage) {
    return () => {
      if (confirm(`You are about to delete stage ${stage.stage}, are you sure you want to proceed?`)) {
        const newStages = this.props.pipeline.stages.filter((s) => s.stage !== stage.stage);

        this.props.onStagesChange(newStages);
      }
    };
  },

  _formatConnectedStreams(streams) {
    const formattedStreams = streams.map((s) => `"${s.title}"`);
    const streamList = streams.length > 1 ? [formattedStreams.slice(0, -1).join(', '), formattedStreams.slice(-1)].join(' and ') : formattedStreams[0];

    return (
      <span>
        This pipeline is processing messages from the{' '}
        <Pluralize singular="stream" plural="streams" value={streams.length} />{' '}
        {streamList}.
      </span>
    );
  },

  _formatStage(stage, maxStage) {
    return (
      <Stage key={`stage-${stage.stage}`}
             pipeline={this.props.pipeline}
             stage={stage}
             isLastStage={stage.stage === maxStage}
             onUpdate={this._updateStage(stage)}
             onDelete={this._deleteStage(stage)} />
    );
  },

  render() {
    const { pipeline } = this.props;

    const maxStage = pipeline.stages.reduce((max, currentStage) => Math.max(max, currentStage.stage), -Infinity);
    const formattedStages = pipeline.stages
      .sort((s1, s2) => s1.stage - s2.stage)
      .map((stage) => this._formatStage(stage, maxStage));

    return (
      <div>
        {this._connections_warning()}
        <PipelineDetails pipeline={pipeline} onChange={this.props.onPipelineChange} />
        <Row className="row-sm row-margin-top">
          <Col md={12}>
            <div className="pull-right">
              <PipelineConnectionsForm pipeline={pipeline}
                                       connections={this.props.connections}
                                       streams={this.props.streams}
                                       save={this.props.onConnectionsChange} />
            </div>
            <h2>Pipeline connections</h2>
            <p className="description-margin-top">
              <PipelineConnectionsList pipeline={pipeline}
                                       connections={this.props.connections}
                                       streams={this.props.streams}
                                       streamsFormatter={this._formatConnectedStreams}
                                       noConnectionsMessage="Select streams that will be processed by this pipeline." />
            </p>
            <hr />
          </Col>
        </Row>
        <Row className="row-sm row-margin-top">
          <Col md={12}>
            <div className="pull-right">
              <StageForm create save={this._saveStage} />
            </div>
            <h2>Pipeline Stages</h2>
            <p className="description-margin-top">
              Stages are groups of conditions and actions which need to run in order, and provide the necessary{' '}
              control flow to decide whether or not to run the rest of a pipeline.
            </p>
          </Col>
        </Row>
        <EntityList bsNoItemsStyle="info" noItemsText="There are no rules on this stage." items={formattedStages} />
      </div>
    );
  },
});

export default Pipeline;
