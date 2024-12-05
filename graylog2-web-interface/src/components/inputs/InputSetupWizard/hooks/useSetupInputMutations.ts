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

const updateRouting = async (params : RoutingParams) => {
  const url = qualifyUrl(ApiRoutes.PipelinesController.updateRouting().url);

  return fetch('PUT', url, { ...params, remove_from_default: false }); // TODO: remove remove_from_default after API change
};

const usePipelineRoutingMutation = () => {
  const createStreamMutation = useMutation(createStream);
  const startStreamMutation = useMutation(startStream);
  const createPipelineMutation = useMutation(createPipeline);
  const updateRoutingMutation = useMutation(updateRouting);

  return ({
    createStreamMutation,
    startStreamMutation,
    createPipelineMutation,
    updateRoutingMutation,
  });
};

export default usePipelineRoutingMutation;
