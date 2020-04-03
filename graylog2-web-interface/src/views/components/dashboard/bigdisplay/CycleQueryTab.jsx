// @flow strict
import { useEffect } from 'react';
import View from 'views/logic/views/View';
import type { QueryId } from 'views/logic/queries/Query';
import { ViewActions } from 'views/stores/ViewStore';

type Props = {
  interval: number,
  view: ?View,
  activeQuery: ?QueryId,
  tabs?: ?Array<number>,
};

const CycleQueryTab = ({ interval, view, activeQuery, tabs }: Props) => {
  useEffect(() => {
    const cycleInterval = setInterval(() => {
      if (!view || !activeQuery) {
        return;
      }
      const queryTabs = tabs || view.search.queries.toIndexedSeq().map((_, v) => v).toJS();
      const currentQueryIndex = view.search.queries.toIndexedSeq().findIndex((q) => q.id === activeQuery);
      const currentTabIndex = queryTabs.indexOf(currentQueryIndex);
      const nextQueryIndex = queryTabs[(currentTabIndex + 1) % queryTabs.length];
      const nextQueryId = view.search.queries.toIndexedSeq().get(nextQueryIndex).id;
      if (nextQueryId !== activeQuery) {
        ViewActions.selectQuery(nextQueryId);
      }
    }, interval * 1000);

    return () => clearInterval(cycleInterval);
  }, [interval, view, activeQuery, tabs]);

  return null;
};

export default CycleQueryTab;
