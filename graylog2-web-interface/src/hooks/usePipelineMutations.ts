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

import { useMutation, useQueryClient } from '@tanstack/react-query';

import { PipelinesPipelines } from '@graylog/server-api';

import type { NewPipelineType, PipelineType } from 'components/pipelines/types';
import { DEFAULT_PIPELINE } from 'components/pipelines/types';
import UserNotification from 'util/UserNotification';
import SourceGenerator from 'logic/pipelines/SourceGenerator';

import { PIPELINES_QUERY_KEY } from './usePipelines';
import { PIPELINE_QUERY_KEY } from './usePipeline';

const createPipeline = async ({ pipelineSource }: { pipelineSource: NewPipelineType }): Promise<PipelineType> => {
  const requestPipeline = {
    ...DEFAULT_PIPELINE,
    ...pipelineSource,
  };

  return PipelinesPipelines.createFromParser({
    ...requestPipeline,
    source: SourceGenerator.generatePipeline(requestPipeline),
  });
};

const updatePipeline = async ({ pipelineSource, pipelineId }: { pipelineSource: PipelineType; pipelineId: string }) =>
  PipelinesPipelines.update(pipelineSource, pipelineId);

const deletePipeline = async ({ pipelineId }: { pipelineId: string }) => PipelinesPipelines.remove(pipelineId);

const usePipelineMutations = () => {
  const queryClient = useQueryClient();

  const createMutation = useMutation(createPipeline, {
    onError: (errorThrown) => {
      UserNotification.error(`Creating pipeline failed with status: ${errorThrown}`, 'Could not create pipeline');
    },
    onSuccess: () => {
      UserNotification.success('Pipeline has been successfully created.', 'Success!');
      queryClient.invalidateQueries(PIPELINES_QUERY_KEY);
    },
  });

  const updateMutation = useMutation(updatePipeline, {
    onError: (errorThrown) => {
      UserNotification.error(`Updating pipeline failed with status: ${errorThrown}`, 'Could not update pipeline');
    },
    onSuccess: () => {
      UserNotification.success('Pipeline has been successfully updated.', 'Success!');
      queryClient.invalidateQueries(PIPELINES_QUERY_KEY);
      queryClient.invalidateQueries(PIPELINE_QUERY_KEY);
    },
  });

  const deleteMutation = useMutation(deletePipeline, {
    onError: (errorThrown) => {
      UserNotification.error(`Deleting pipeline failed with status: ${errorThrown}`, 'Could not delete pipeline');
    },
    onSuccess: () => {
      UserNotification.success('Pipeline has been successfully deleted.', 'Success!');
      queryClient.invalidateQueries(PIPELINES_QUERY_KEY);
      queryClient.invalidateQueries(PIPELINE_QUERY_KEY);
    },
  });

  return {
    createPipeline: createMutation.mutateAsync,
    updatePipeline: updateMutation.mutateAsync,
    deletePipeline: deleteMutation.mutateAsync,
  };
};

export default usePipelineMutations;
