import { useMutation } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import type { Stream } from 'logic/streams/types';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';

export type RoutingParams = {
  stream_id?: string,
  input_id: string,
}

export type StreamConfiguration = Pick<Stream,
  'index_set_id' |
  'title' |
  'remove_matches_from_default_stream'
> & Partial<Pick<Stream, 'description'>>

type PipelineConfiguration = Pick<PipelineType,
  'title' |
  'description' |
  'source'
>

const createStream = async (stream: StreamConfiguration): Promise<{ stream_id: string }> => {
  const url = qualifyUrl(ApiRoutes.StreamsApiController.create().url);

  return fetch('POST', url, stream);
};

const startStream = async (streamId) => {
  const url = qualifyUrl(ApiRoutes.StreamsApiController.resume(streamId).url);

  return fetch('POST', url);
};

const createPipeline = (pipeline: PipelineConfiguration) : Promise<PipelineType> => {
  const url = qualifyUrl(ApiRoutes.PipelinesController.create().url);

  return fetch('POST', url, pipeline);
};

const updateRouting = async (params: RoutingParams): Promise <{ id: string }> => {
  const url = qualifyUrl(ApiRoutes.PipelinesController.updateRouting().url);

  return fetch('PUT', url, params);
};

const deleteStream = async (streamId: string) => {
  const url = qualifyUrl(ApiRoutes.StreamsApiController.delete(streamId).url);

  return fetch('DELETE', url);
};

const deletePipeline = async (pipelineId: string) => {
  const url = qualifyUrl(ApiRoutes.PipelinesController.delete(pipelineId).url);

  return fetch('DELETE', url);
};

const deleteRoutingRule = async (ruleId: string) => {
  const url = qualifyUrl(ApiRoutes.RulesController.delete(ruleId).url);

  return fetch('DELETE', url);
};

const usePipelineRoutingMutation = () => {
  const createStreamMutation = useMutation(createStream);
  const startStreamMutation = useMutation(startStream);
  const createPipelineMutation = useMutation(createPipeline);
  const updateRoutingMutation = useMutation(updateRouting);
  const deleteStreamMutation = useMutation(deleteStream);
  const deletePipelineMutation = useMutation(deletePipeline);
  const deleteRoutingRuleMutation = useMutation(deleteRoutingRule);

  return ({
    createStreamMutation,
    startStreamMutation,
    createPipelineMutation,
    updateRoutingMutation,
    deleteStreamMutation,
    deletePipelineMutation,
    deleteRoutingRuleMutation,
  });
};

export default usePipelineRoutingMutation;
