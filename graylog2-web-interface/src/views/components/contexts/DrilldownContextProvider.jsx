// @flow strict
import * as React from 'react';
import { useContext } from 'react';

import connect from 'stores/connect';
import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import Query, { createElasticsearchQueryString, filtersToStreamSet } from 'views/logic/queries/Query';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import GlobalOverride from 'views/logic/search/GlobalOverride';

import DrilldownContext from './DrilldownContext';
import ViewTypeContext from './ViewTypeContext';
import type { Drilldown } from './DrilldownContext';

type Props = {
  children: React.Node,
  widget: Widget,
  globalOverride: ?GlobalOverride,
  currentQuery: Query,
};

const DrilldownContextProvider = ({ children, widget, globalOverride, currentQuery }: Props) => {
  const viewType = useContext(ViewTypeContext);

  if (viewType === View.Type.Dashboard) {
    const { streams, timerange, query } = widget;
    const value: Drilldown = {
      streams,
      timerange: (globalOverride && globalOverride.timerange ? globalOverride.timerange : timerange) || { type: 'relative', range: 300 },
      query: (globalOverride && globalOverride.query ? globalOverride.query : query) || createElasticsearchQueryString(''),
    };
    return <DrilldownContext.Provider value={value}>{children}</DrilldownContext.Provider>;
  }
  if (currentQuery) {
    const streams = filtersToStreamSet(currentQuery.filter).toJS();
    const { timerange, query } = currentQuery;
    const value: Drilldown = { streams, timerange, query };
    return <DrilldownContext.Provider value={value}>{children}</DrilldownContext.Provider>;
  }
  return children;
};

export default connect(
  DrilldownContextProvider,
  {
    currentQuery: CurrentQueryStore,
    globalOverride: GlobalOverrideStore,
  },
);
