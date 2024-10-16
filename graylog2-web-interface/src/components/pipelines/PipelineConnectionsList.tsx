import React from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';
import type { PipelineConnectionsType } from 'stores/pipelines/PipelineConnectionsStore';
import type { Stream } from 'stores/streams/StreamsStore';

type Props = {
  pipeline: PipelineType,
  connections: PipelineConnectionsType[],
  streams: Stream[],
  streamsFormatter: (streams: Stream[]) => React.ReactNode,
  noConnectionsMessage?: React.ReactNode,
}

const PipelineConnectionsList = ({ pipeline, connections, streams, streamsFormatter, noConnectionsMessage = 'Pipeline not connected to any streams' }: Props) => {
  const streamsUsingPipeline = connections
    .filter((c) => c.pipeline_ids && c.pipeline_ids.includes(pipeline.id)) // Get connections for this pipeline
    .filter((c) => streams.some((s) => s.id === c.stream_id)) // Filter out deleted streams
    .map((c) => streams.find((s) => s.id === c.stream_id))
    .sort((s1, s2) => naturalSort(s1.title, s2.title));

  return (
    <span>
      {streamsUsingPipeline.length === 0 ? noConnectionsMessage : streamsFormatter(streamsUsingPipeline)}
    </span>
  );
};

export default PipelineConnectionsList;
