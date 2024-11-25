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
import { useEffect, useState } from 'react';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Pipeline from 'components/pipelines/Pipeline';
import NewPipeline from 'components/pipelines/NewPipeline';
import SourceGenerator from 'logic/pipelines/SourceGenerator';
import { StreamsStore } from 'stores/streams/StreamsStore';
import { PipelineConnectionsStore, PipelineConnectionsActions } from 'stores/pipelines/PipelineConnectionsStore';
import { PipelinesStore, PipelinesActions } from 'stores/pipelines/PipelinesStore';
import DocsHelper from 'util/DocsHelper';
import { useStore } from 'stores/connect';
import useParams from 'routing/useParams';
import { RulesActions } from 'stores/rules/RulesStore';

import PipelinesPageNavigation from '../components/pipelines/PipelinesPageNavigation';

const _isNewPipeline = (pipelineId: string) => pipelineId === 'new';

const PipelineDetailsPage = () => {
  const params = useParams<{ pipelineId: string }>();
  const pipeline = useStore(PipelinesStore, (state) => state.pipelines?.filter((p) => p.id === params.pipelineId)?.[0]);
  const connections = useStore(PipelineConnectionsStore, (state) => state.connections?.filter(
    (c) => c.pipeline_ids && c.pipeline_ids.includes(params.pipelineId),
  ));
  const [streams, setStreams] = useState();

  useEffect(() => {
    if (!_isNewPipeline(params.pipelineId)) {
      PipelinesActions.get(params.pipelineId);
    }
  }, [params.pipelineId]);

  useEffect(() => {
    RulesActions.list();
    PipelineConnectionsActions.list();

    StreamsStore.listStreams().then((_streams) => {
      const filteredStreams = _streams.filter((s) => s.is_editable);

      setStreams(filteredStreams);
    });
  }, []);

  const _onConnectionsChange = (updatedConnections, callback) => {
    PipelineConnectionsActions.connectToPipeline(updatedConnections);
    callback();
  };

  const _onStagesChange = (newStages, callback) => {
    const pipelineWithNewStages = {
      ...pipeline,
      stages: newStages,
    };

    const newPipeline = {
      ...pipelineWithNewStages,
      source: SourceGenerator.generatePipeline(pipelineWithNewStages),
    };

    PipelinesActions.update(newPipeline);

    if (typeof callback === 'function') {
      callback();
    }
  };

  const _savePipeline = (_pipeline, callback) => {
    const requestPipeline = {
      ..._pipeline,
      source: SourceGenerator.generatePipeline(_pipeline),
    };

    const promise = requestPipeline.id
      ? PipelinesActions.update(requestPipeline)
      : PipelinesActions.save(requestPipeline);

    promise.then((p) => callback(p));
  };

  const _isLoading = !_isNewPipeline(params.pipelineId) && (!pipeline || !connections || !streams);

  if (_isLoading) {
    return <Spinner />;
  }

  const title = _isNewPipeline(params.pipelineId)
    ? 'New pipeline'
    : <span>Pipeline <em>{pipeline.title}</em></span>;

  const content = _isNewPipeline(params.pipelineId)
    ? <NewPipeline onChange={_savePipeline} />
    : (
      <Pipeline pipeline={pipeline}
                connections={connections}
                streams={streams}
                onConnectionsChange={_onConnectionsChange}
                onStagesChange={_onStagesChange}
                onPipelineChange={_savePipeline} />
    );

  const pageTitle = _isNewPipeline(params.pipelineId) ? 'New pipeline' : `Pipeline ${pipeline.title}`;

  return (
    <DocumentTitle title={pageTitle}>
      <div>
        <PipelinesPageNavigation />
        <PageHeader title={title}
                    documentationLink={{
                      title: 'Pipelines documentation',
                      path: DocsHelper.PAGES.PIPELINES,
                    }}>
          <span>
            Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where
            rules are evaluated and applied. Messages can go through one or more stages.
            <br />
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
};

export default PipelineDetailsPage;
