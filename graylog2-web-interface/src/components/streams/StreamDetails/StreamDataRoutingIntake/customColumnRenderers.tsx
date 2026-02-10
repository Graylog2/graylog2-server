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

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { StreamConnectedPipeline } from 'components/streams/StreamDetails/StreamDataRoutingIntake/types';

import ConnectedPipelineLinkedCell from './cells/ConnectedPipelineLinkedCell';
import ConnectedPipelineStreamsCell from './cells/ConnectedPipelineStreamsCell';

const customColumnRenderers: ColumnRenderers<StreamConnectedPipeline> = {
  attributes: {
    pipeline: {
      renderCell: (pipeline: StreamConnectedPipeline['pipeline'], connectedPipeline: StreamConnectedPipeline) => (
        <ConnectedPipelineLinkedCell title={pipeline} id={connectedPipeline.pipeline_id} type="pipeline" />
      ),
    },
    rule: {
      renderCell: (rule: StreamConnectedPipeline['rule'], connectedPipeline: StreamConnectedPipeline) => (
        <ConnectedPipelineLinkedCell title={rule} id={connectedPipeline.rule_id} type="rule" />
      ),
    },
    connected_streams: {
      renderCell: (connected_streams: StreamConnectedPipeline['connected_streams']) => (
        <ConnectedPipelineStreamsCell streams={connected_streams} />
      ),
    },
  },
};

export default customColumnRenderers;
