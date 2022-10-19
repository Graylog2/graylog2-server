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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Pipeline from 'components/pipelines/Pipeline';
import NewPipeline from 'components/pipelines/NewPipeline';
import SourceGenerator from 'logic/pipelines/SourceGenerator';
import ObjectUtils from 'util/ObjectUtils';
import withParams from 'routing/withParams';
import { StreamsStore } from 'stores/streams/StreamsStore';
import { PipelineConnectionsStore, PipelineConnectionsActions } from 'stores/pipelines/PipelineConnectionsStore';
import { PipelinesStore, PipelinesActions } from 'stores/pipelines/PipelinesStore';
import { RulesStore } from 'stores/rules/RulesStore';

import PipelinesPageNavigation from '../components/pipelines/PipelinesPageNavigation';

function filterPipeline(state) {
  return state.pipelines ? state.pipelines.filter((p) => p.id === this.props.params.pipelineId)[0] : undefined;
}

function filterConnections(state) {
  if (!state.connections) {
    return undefined;
  }

  return state.connections.filter((c) => c.pipeline_ids && c.pipeline_ids.includes(this.props.params.pipelineId));
}

const PipelineDetailsPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'PipelineDetailsPage',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connectFilter(PipelinesStore, 'pipeline', filterPipeline), Reflux.connectFilter(PipelineConnectionsStore, 'connections', filterConnections)],

  componentDidMount() {
    const { params } = this.props;

    if (!this._isNewPipeline(params.pipelineId)) {
      PipelinesActions.get(params.pipelineId);
    }

    RulesStore.list();
    PipelineConnectionsActions.list();

    StreamsStore.listStreams().then((streams) => {
      const filteredStreams = streams.filter((s) => s.is_editable);

      this.setState({ streams: filteredStreams });
    });
  },

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (!this._isNewPipeline(nextProps.params.pipelineId)) {
      PipelinesActions.get(nextProps.params.pipelineId);
    }
  },

  _onConnectionsChange(updatedConnections, callback) {
    PipelineConnectionsActions.connectToPipeline(updatedConnections);
    callback();
  },

  _onStagesChange(newStages, callback) {
    const { pipeline } = this.state;
    const newPipeline = ObjectUtils.clone(pipeline);

    newPipeline.stages = newStages;
    newPipeline.source = SourceGenerator.generatePipeline(newPipeline);
    PipelinesActions.update(newPipeline);

    if (typeof callback === 'function') {
      callback();
    }
  },

  _savePipeline(pipeline, callback) {
    const requestPipeline = ObjectUtils.clone(pipeline);

    requestPipeline.source = SourceGenerator.generatePipeline(pipeline);
    let promise;

    if (requestPipeline.id) {
      promise = PipelinesActions.update(requestPipeline);
    } else {
      promise = PipelinesActions.save(requestPipeline);
    }

    promise.then((p) => callback(p));
  },

  _isNewPipeline(pipelineId) {
    return pipelineId === 'new';
  },

  _isLoading() {
    const { params } = this.props;
    const { connections, streams, pipeline } = this.state;

    return !this._isNewPipeline(params.pipelineId) && (!pipeline || !connections || !streams);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { params } = this.props;
    const { connections, streams, pipeline, rules } = this.state;

    let title;

    if (this._isNewPipeline(params.pipelineId)) {
      title = 'New pipeline';
    } else {
      title = <span>Pipeline <em>{pipeline.title}</em></span>;
    }

    let content;

    if (this._isNewPipeline(params.pipelineId)) {
      content = <NewPipeline onChange={this._savePipeline} />;
    } else {
      content = (
        <Pipeline pipeline={pipeline}
                  connections={connections}
                  streams={streams}
                  rules={rules}
                  onConnectionsChange={this._onConnectionsChange}
                  onStagesChange={this._onStagesChange}
                  onPipelineChange={this._savePipeline} />
      );
    }

    const pageTitle = (this._isNewPipeline(params.pipelineId) ? 'New pipeline' : `Pipeline ${pipeline.title}`);

    return (
      <DocumentTitle title={pageTitle}>
        <div>
          <PipelinesPageNavigation />
          <PageHeader title={title}>
            <span>
              Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where
              rules are evaluated and applied. Messages can go through one or more stages.
            </span>
            <span>
              After each stage is completed, you can decide if messages matching all or one of the rules continue to
              the next stage.
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              {content}
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(PipelineDetailsPage);
