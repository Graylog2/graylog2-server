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
import { useMutation } from '@tanstack/react-query';

import { PipelinesPipelines, Streams, PipelinesRules, PipelinesConnections } from '@graylog/server-api';

import SourceGenerator from 'logic/pipelines/SourceGenerator';
import type { Stream } from 'logic/streams/types';
import type { NewPipelineType, PipelineType } from 'components/pipelines/types';
import { DEFAULT_PIPELINE } from 'components/pipelines/types';

export type RoutingParams = {
  stream_id?: string;
  input_id: string;
  remove_from_default?: boolean;
};

export type StreamConfiguration = Pick<Stream, 'index_set_id' | 'title' | 'remove_matches_from_default_stream'> &
  Partial<Pick<Stream, 'description'>>;

const createStream = async (stream: StreamConfiguration): Promise<{ stream_id: string }> =>
  Streams.create({
    matching_type: undefined,
    rules: undefined,
    content_pack: undefined,
    description: undefined,
    share_request: undefined,
    ...stream,
  });

const startStream = async (streamId) => Streams.resume(streamId);

const createPipeline = (pipeline: NewPipelineType): Promise<PipelineType> => {
  const requestPipeline = {
    ...DEFAULT_PIPELINE,
    ...pipeline,
  };

  return PipelinesPipelines.createFromParser({
    ...requestPipeline,
    source: SourceGenerator.generatePipeline(requestPipeline),
  });
};

const updateRouting = async (params: RoutingParams): Promise<{ rule_id: string }> =>
  PipelinesPipelines.routing({ remove_from_default: undefined, stream_id: undefined, ...params });

const deleteStream = async (streamId: string) => Streams.remove(streamId);

const deletePipeline = async (pipelineId: string) => PipelinesPipelines.remove(pipelineId);

const deleteRoutingRule = async (ruleId: string) => PipelinesRules.remove(ruleId);
const connectPipeline = async ({ pipelineId, streamId }: { pipelineId: string; streamId: string }) =>
  PipelinesConnections.connectStreams({ stream_ids: [streamId], pipeline_id: pipelineId });

const useSetupInputMutations = () => {
  const createStreamMutation = useMutation(createStream);
  const startStreamMutation = useMutation(startStream);
  const createPipelineMutation = useMutation(createPipeline);
  const updateRoutingMutation = useMutation(updateRouting);
  const deleteStreamMutation = useMutation(deleteStream);
  const deletePipelineMutation = useMutation(deletePipeline);
  const deleteRoutingRuleMutation = useMutation(deleteRoutingRule);
  const connectPipelineMutation = useMutation(connectPipeline);

  return {
    createStreamMutation,
    startStreamMutation,
    createPipelineMutation,
    updateRoutingMutation,
    deleteStreamMutation,
    deletePipelineMutation,
    deleteRoutingRuleMutation,
    connectPipelineMutation,
  };
};

export default useSetupInputMutations;
