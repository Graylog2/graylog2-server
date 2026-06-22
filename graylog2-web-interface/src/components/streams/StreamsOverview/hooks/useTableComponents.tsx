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

import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type { Stream } from 'logic/streams/types';
import StreamActions from 'components/streams/StreamsOverview/StreamActions';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';
import ExpandedRulesSection from 'components/streams/StreamsOverview/ExpandedRulesSection';
import ExpandedRulesActions from 'components/streams/StreamsOverview/ExpandedRulesActions';
import ExpandedDestinationFilterRulesSection from 'components/streams/StreamsOverview/ExpandedDestinationFilterRulesSection';
import ExpandedDestinationFilterRulesActions from 'components/streams/StreamsOverview/ExpandedDestinationFilterRulesActions';
import ExpandedAssociatedInputsSection from 'components/streams/StreamsOverview/ExpandedAssociatedInputsSection';
import ExpandedPipelinesSection from 'components/streams/StreamsOverview/ExpandedPipelinesSection';
import ExpandedPipelinesActions from 'components/streams/StreamsOverview/ExpandedPipelinesActions';
import ExpandedRoutingPipelinesSection from 'components/streams/StreamsOverview/ExpandedRoutingPipelinesSection';
import { METRIC_COLUMN_IDS, METRIC_COLUMN_TITLES } from 'components/streams/StreamsOverview/metricColumns';
import ExpandedOutputsSection from 'components/streams/StreamsOverview/ExpandedOutputsSection';
import ExpandedOutputsActions from 'components/streams/StreamsOverview/ExpandedOutputsActions';
import type { ExpandedSectionRenderer } from 'components/common/EntityDataTable/types';

const useTableElements = ({
  indexSets,
  pluggableExpandedSections,
}: {
  indexSets: Array<IndexSet>;
  pluggableExpandedSections: { [sectionName: string]: ExpandedSectionRenderer<Stream> };
}) => {
  const entityActions = (listItem: Stream) => <StreamActions stream={listItem} indexSets={indexSets} />;

  const renderExpandedRules = (stream: Stream) => <ExpandedRulesSection stream={stream} />;
  const renderExpandedRulesActions = (stream: Stream) => <ExpandedRulesActions stream={stream} />;
  const expandedSections = {
    rules: {
      title: 'Rules',
      content: renderExpandedRules,
      actions: renderExpandedRulesActions,
    },
    destination_filters: {
      title: 'Filter Rules',
      content: (stream: Stream) => <ExpandedDestinationFilterRulesSection stream={stream} />,
      actions: (stream: Stream) => <ExpandedDestinationFilterRulesActions stream={stream} />,
    },
    outputs: {
      title: 'Outputs',
      content: (stream: Stream) => <ExpandedOutputsSection stream={stream} />,
      actions: (stream: Stream) => <ExpandedOutputsActions stream={stream} />,
    },
    [METRIC_COLUMN_IDS.associatedInputs]: {
      title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.associatedInputs],
      content: (stream: Stream) => <ExpandedAssociatedInputsSection stream={stream} />,
    },
    [METRIC_COLUMN_IDS.pipelines]: {
      title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.pipelines],
      content: (stream: Stream) => <ExpandedPipelinesSection stream={stream} />,
      actions: () => <ExpandedPipelinesActions />,
    },
    [METRIC_COLUMN_IDS.routingPipelines]: {
      title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.routingPipelines],
      content: (stream: Stream) => <ExpandedRoutingPipelinesSection stream={stream} />,
    },
    ...pluggableExpandedSections,
  };

  return {
    entityActions,
    bulkActions: <BulkActions indexSets={indexSets} />,
    expandedSections,
  };
};

export default useTableElements;
