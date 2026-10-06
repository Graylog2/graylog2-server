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
import { useMemo } from 'react';
import URI from 'urijs';

import generateId from 'logic/generateId';
import useCreateSearch from 'views/hooks/useCreateSearch';
import Routes from 'routing/Routes';
import { nonInfoPriorities } from 'components/events/Constants';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import Search from 'views/logic/search/Search';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import { timeRangeToQueryParameter } from 'views/logic/TimeRange';
import { serializeTimeRange } from 'components/common/EntityFilters/helpers/timeRange';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import type Widget from 'views/logic/widgets/Widget';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';

export const DEFAULT_TIME_RANGE_SECONDS = 86400;

export type WelcomeSearchWidgetEntry = {
  title: string;
  widget: Widget;
  position: WidgetPosition;
};

export const messagesTodayLink = (timeRange: RelativeTimeRangeWithEnd) =>
  new URI(Routes.SEARCH).addQuery({ q: '', ...timeRangeToQueryParameter(timeRange) }).toString();

export const alertsOrEventsLink = (timeRange: RelativeTimeRangeWithEnd, alert: boolean) =>
  new URI(Routes.ALERTS.LIST)
    .addQuery('page', 1)
    .addQuery('filters', [
      ...nonInfoPriorities.map((priority) => `priority=${priority}`),
      `timestamp=${serializeTimeRange(timeRange)}`,
      `alert=${alert}`,
    ])
    .toString();

export const numberWidget = ({
  title,
  timeRange,
  queryString = '',
  streams = [],
  link,
}: {
  title: string;
  timeRange: RelativeTimeRangeWithEnd;
  queryString?: string;
  streams?: Array<string>;
  link?: string;
}): { title: string; widget: Widget } => ({
  title,
  widget: AggregationWidget.builder()
    .id(generateId())
    .timerange(timeRange)
    .context(link)
    .query(createElasticsearchQueryString(queryString))
    .streams(streams)
    .config(
      AggregationWidgetConfig.builder()
        .series([Series.forFunction('count()')])
        .visualization('numeric')
        .visualizationConfig(NumberVisualizationConfig.create(true, 'NEUTRAL', 'bottom-left'))
        .rollup(false)
        .build(),
    )
    .build(),
});

const buildViewState = (entries: Array<WelcomeSearchWidgetEntry>) =>
  ViewState.create()
    .toBuilder()
    .titles({ widget: Object.fromEntries(entries.map(({ widget, title }) => [widget.id, title])) })
    .widgets(entries.map(({ widget }) => widget))
    .widgetPositions(Object.fromEntries(entries.map(({ widget, position }) => [widget.id, position])))
    .build();

const buildView = (entries: Array<WelcomeSearchWidgetEntry>, timeRange: RelativeTimeRangeWithEnd) => {
  const query = QueryGenerator(undefined, undefined, undefined, timeRange);
  const search = Search.create().toBuilder().queries([query]).build();
  const view = View.create()
    .toBuilder()
    .newId()
    .type(View.Type.Dashboard)
    .state({ [query.id]: buildViewState(entries) })
    .search(search)
    .build();

  return Promise.resolve(UpdateSearchForWidgets(view));
};

/**
 * Turns an arbitrary list of welcome-page widgets into a created search, ready to hand to `SearchPage`.
 * Shared by the general tab (`useWelcomeMetricsSearch`) and any other welcome-page widget area (e.g. the
 * Security tab's search-backed widgets) that needs its own view/search built from a small widget set.
 */
const useWelcomeSearch = (entries: Array<WelcomeSearchWidgetEntry>, timeRange: RelativeTimeRangeWithEnd) =>
  useCreateSearch(
    // `useCreateSearch` creates a new search on the server for every new promise it receives, so the promise
    // must only be recreated when the widgets or the time range actually change.
    useMemo(() => buildView(entries, timeRange), [entries, timeRange]),
  );

export default useWelcomeSearch;
