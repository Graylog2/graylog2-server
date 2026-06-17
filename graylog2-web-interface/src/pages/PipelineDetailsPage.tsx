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

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Pipeline from 'components/pipelines/Pipeline';
import NewPipeline from 'components/pipelines/NewPipeline';
import SourceGenerator from 'logic/pipelines/SourceGenerator';
import useAllStreams from 'components/streams/hooks/useAllStreams';
import type { Stream } from 'logic/streams/types';
import type { PipelineType, StageType } from 'components/pipelines/types';
import DocsHelper from 'util/DocsHelper';
import useParams from 'routing/useParams';
import usePipeline from 'hooks/usePipeline';
import usePipelineMutations from 'hooks/usePipelineMutations';
import usePipelineConnections, { usePipelineConnectionMutation } from 'hooks/usePipelineConnections';
import {
  EnableDebugMetricsButton,
  ProcessingLoadDebugMetricsBanner,
  ProcessingLoadProvider,
} from 'components/pipelines/processing-load';

import PipelinesPageNavigation from '../components/pipelines/PipelinesPageNavigation';

const _isNewPipeline = (pipelineId: string) => pipelineId === 'new';

const PipelineDetailsPage = () => {
  const params = useParams<{ pipelineId: string }>();
  const isNewPipeline = _isNewPipeline(params.pipelineId);
  const { data: pipeline } = usePipeline(params?.pipelineId, {
    enabled: !isNewPipeline && !!params?.pipelineId,
  });

  const { createPipeline, updatePipeline } = usePipelineMutations();
  const { data: allConnections } = usePipelineConnections();
  const connections = allConnections?.filter((c) => c.pipeline_ids && c.pipeline_ids.includes(params.pipelineId));
  const { connectToPipeline } = usePipelineConnectionMutation();
  const { data: allStreams } = useAllStreams();
  const streams = useMemo(
    () => (allStreams ? allStreams.filter((s: Stream) => s.is_editable) : undefined),
    [allStreams],
  );

  const _onConnectionsChange = (
    updatedConnections: { pipeline: string; streams: Array<string> },
    callback: () => void,
  ) => {
    connectToPipeline(updatedConnections).then(() => callback());
  };

  const _onStagesChange = (newStages: Array<StageType>, callback: () => void) => {
    const pipelineWithNewStages = {
      ...pipeline,
      stages: newStages,
    };

    const newPipeline = {
      ...pipelineWithNewStages,
      source: SourceGenerator.generatePipeline(pipelineWithNewStages),
    };

    updatePipeline({ pipelineSource: newPipeline, pipelineId: newPipeline.id });

    if (typeof callback === 'function') {
      callback();
    }
  };

  const _savePipeline = (_pipeline: PipelineType, callback: (pipeline: PipelineType) => void) => {
    const requestPipeline = {
      ..._pipeline,
      source: SourceGenerator.generatePipeline(_pipeline),
    };

    if (requestPipeline.id) {
      updatePipeline({ pipelineSource: requestPipeline, pipelineId: requestPipeline.id }).then((p) => callback(p));
    } else {
      createPipeline({ pipelineSource: requestPipeline }).then((p) => callback(p));
    }
  };

  const _isLoading = !isNewPipeline && (!pipeline || !connections || !streams);

  if (_isLoading) {
    return <Spinner />;
  }

  const title = isNewPipeline ? (
    'New pipeline'
  ) : (
    <span>
      Pipeline <em>{pipeline.title}</em>
    </span>
  );

  const content = isNewPipeline ? (
    <NewPipeline onChange={_savePipeline} />
  ) : (
    <Pipeline
      pipeline={pipeline}
      connections={connections}
      streams={streams}
      onConnectionsChange={_onConnectionsChange}
      onStagesChange={_onStagesChange}
      onPipelineChange={_savePipeline}
    />
  );

  const pageTitle = isNewPipeline ? 'New pipeline' : `Pipeline ${pipeline.title}`;

  return (
    <DocumentTitle title={pageTitle}>
      <div>
        <PipelinesPageNavigation />
        <PageHeader
          title={title}
          actions={<EnableDebugMetricsButton />}
          documentationLink={{
            title: 'Pipelines documentation',
            path: DocsHelper.PAGES.PIPELINES,
          }}>
          <span>
            Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where
            rules are evaluated and applied. Messages can go through one or more stages.
            <br />
            After each stage is completed, you can decide if messages matching all or one of the rules continue to the
            next stage.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {isNewPipeline ? (
              content
            ) : (
              <ProcessingLoadProvider>
                <ProcessingLoadDebugMetricsBanner />
                {content}
              </ProcessingLoadProvider>
            )}
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default PipelineDetailsPage;
