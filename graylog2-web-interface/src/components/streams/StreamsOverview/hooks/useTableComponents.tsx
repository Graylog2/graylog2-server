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

  const bulkActions = useCallback((
    selectedStreamIds: Array<string>,
    setSelectedStreamIds: (streamIds: Array<string>) => void,
  ) => (
    <BulkActions selectedStreamIds={selectedStreamIds}
                 setSelectedStreamIds={setSelectedStreamIds}
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
    bulkActions,
    expandedSections,
  };
};

export default useTableElements;
