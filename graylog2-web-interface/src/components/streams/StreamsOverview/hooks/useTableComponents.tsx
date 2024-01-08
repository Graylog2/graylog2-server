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
import React, { useCallback, useMemo } from 'react';

import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamActions from 'components/streams/StreamsOverview/StreamActions';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';
import ExpandedRulesSection from 'components/streams/StreamsOverview/ExpandedRulesSection';
import ExpandedRulesActions from 'components/streams/StreamsOverview/ExpandedRulesActions';

const useTableElements = ({ indexSets }: { indexSets: Array<IndexSet> }) => {
  const entityActions = useCallback((listItem: Stream) => (
    <StreamActions stream={listItem}
                   indexSets={indexSets} />
  ), [indexSets]);

  const renderExpandedRules = useCallback((stream: Stream) => (
    <ExpandedRulesSection stream={stream} />
  ), []);
  const renderExpandedRulesActions = useCallback((stream: Stream) => (
    <ExpandedRulesActions stream={stream} />
  ), []);

  const expandedSections = useMemo(() => ({
    rules: {
      title: 'Rules',
      content: renderExpandedRules,
      actions: renderExpandedRulesActions,
    },
  }), [renderExpandedRules, renderExpandedRulesActions]);

  return {
    entityActions,
    bulkActions: <BulkActions indexSets={indexSets} />,
    expandedSections,
  };
};

export default useTableElements;
