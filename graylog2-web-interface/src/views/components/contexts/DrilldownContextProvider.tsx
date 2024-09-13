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

import type Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import type Query from 'views/logic/queries/Query';
import {
  createElasticsearchQueryString,
  filtersToStreamSet,
  filtersToStreamCategorySet,
} from 'views/logic/queries/Query';
import type GlobalOverride from 'views/logic/search/GlobalOverride';
import useViewType from 'views/hooks/useViewType';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { concatQueryStrings } from 'views/logic/queries/QueryHelper';

import DrilldownContext from './DrilldownContext';
import type { Drilldown } from './DrilldownContext';

const useDrillDownContextValue = (widget: Widget, globalOverride: GlobalOverride | undefined, currentQuery: Query): Drilldown => {
  const viewType = useViewType();

  if (viewType === View.Type.Dashboard) {
    const { streams, timerange, query, streamCategories } = widget;
    const dashboardAndWidgetQueryString = globalOverride?.query?.query_string
      ? concatQueryStrings([query?.query_string, globalOverride.query.query_string])
      : query?.query_string;

    return ({
      streams,
      streamCategories,
      timerange: (globalOverride?.timerange ? globalOverride.timerange : timerange) || DEFAULT_TIMERANGE,
      query: createElasticsearchQueryString(dashboardAndWidgetQueryString || ''),
    });
  }

  if (currentQuery) {
    const streams = filtersToStreamSet(currentQuery.filter).toJS();
    const streamCategories = filtersToStreamCategorySet(currentQuery.filter).toJS();
    const { timerange, query } = currentQuery;

    return ({ streams, streamCategories, timerange, query });
  }

  return undefined;
};

type Props = {
  children: React.ReactElement,
  widget: Widget,
};

const DrilldownContextProvider = ({ children, widget }: Props) => {
  const currentQuery = useCurrentQuery();
  const globalOverride = useGlobalOverride();
  const drillDownContextValue = useDrillDownContextValue(widget, globalOverride, currentQuery);

  if (drillDownContextValue) {
    return <DrilldownContext.Provider value={drillDownContextValue}>{children}</DrilldownContext.Provider>;
  }

  return children;
};

export default DrilldownContextProvider;
