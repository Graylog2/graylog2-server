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

import type { ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import type { Output } from 'hooks/useOutputs';
import type { Stream, StreamRule } from 'logic/streams/types';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import IndexSetCell from 'components/streams/StreamsOverview/cells/IndexSetCell';
import TitleCell from 'components/streams/StreamsOverview/cells/TitleCell';
import ThroughputCell from 'components/streams/StreamsOverview/cells/ThroughputCell';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

import StatusCell from './cells/StatusCell';
import StreamRulesCell from './cells/StreamRulesCell';
import PipelinesCell from './cells/PipelinesCell';
import OutputsCell from './cells/OutputsCell';
import ArchivingsCell from './cells/ArchivingsCell';
import DestinationFilterRulesCell from './cells/DestinationFilterRulesCell';
import MessageCountCell from './cells/MessageCountCell';
import AvgProcessingTimeCell from './cells/AvgProcessingTimeCell';
import MaxProcessingTimeCell from './cells/MaxProcessingTimeCell';
import AssociatedInputsCell from './cells/AssociatedInputsCell';
import RoutingPipelinesCell from './cells/RoutingPipelinesCell';
import { METRIC_COLUMN_IDS } from './metricColumns';

const pipelineRenderer = {
  pipelines: {
    renderCell: (_pipeline: any[], stream) => <PipelinesCell stream={stream} />,
    staticWidth: 'matchHeader' as const,
    textAlign: 'right',
  },
};
const customColumnRenderers = (
  indexSets: Array<IndexSet>,
  isPipelineColumnPermitted: boolean,
  extensionColumnRenderers?: ColumnRenderersByAttribute<Stream>,
): ColumnRenderers<Stream> => ({
  attributes: {
    title: {
      renderCell: (_title: string, stream) => <TitleCell stream={stream} />,
      width: 0.5,
    },
    index_set_title: {
      renderCell: (_index_set_title: string, stream) => <IndexSetCell indexSets={indexSets} stream={stream} />,
      width: 0.3,
    },
    throughput: {
      renderCell: (_throughput: string, stream) => <ThroughputCell stream={stream} />,
      staticWidth: 'matchHeader' as const,
      textAlign: 'right',
    },
    disabled: {
      renderCell: (_disabled: string, stream) => <StatusCell stream={stream} />,
      staticWidth: 100,
    },
    rules: {
      renderCell: (_rules: StreamRule[], stream) => <StreamRulesCell stream={stream} />,
      staticWidth: 'matchHeader' as const,
      textAlign: 'right',
    },
    ...(isPipelineColumnPermitted ? pipelineRenderer : {}),
    outputs: {
      renderCell: (_outputs: Output[], stream) => <OutputsCell stream={stream} />,
      staticWidth: 'matchHeader' as const,
    },
    destination_filters: {
      renderCell: (_destinationFilters: string, stream) => <DestinationFilterRulesCell stream={stream} />,
      staticWidth: 'matchHeader' as const,
    },
    archiving: {
      renderCell: (_archiving: boolean, stream) => <ArchivingsCell stream={stream} indexSets={indexSets} />,
      staticWidth: 'matchHeader' as const,
    },
    [METRIC_COLUMN_IDS.messageCount]: {
      renderCell: (_value: unknown, stream) => <MessageCountCell stream={stream} />,
      staticWidth: 180,
    },
    [METRIC_COLUMN_IDS.avgProcessingTime]: {
      renderCell: (_value: unknown, stream) => <AvgProcessingTimeCell stream={stream} />,
      staticWidth: 200,
    },
    [METRIC_COLUMN_IDS.maxProcessingTime]: {
      renderCell: (_value: unknown, stream) => <MaxProcessingTimeCell stream={stream} />,
      staticWidth: 200,
    },
    [METRIC_COLUMN_IDS.associatedInputs]: {
      renderCell: (_value: unknown, stream) => <AssociatedInputsCell stream={stream} />,
      staticWidth: 180,
      textAlign: 'right',
    },
    [METRIC_COLUMN_IDS.routingPipelines]: {
      renderCell: (_value: unknown, stream) => <RoutingPipelinesCell stream={stream} />,
      staticWidth: 160,
      textAlign: 'right',
    },
    ...(extensionColumnRenderers || {}),
  },
});

export default customColumnRenderers;
