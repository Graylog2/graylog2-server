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
import usePermissions from 'hooks/usePermissions';
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
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import FieldType from 'views/logic/fieldtypes/FieldType';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { TIMESTAMP_FIELD } from 'views/Constants';

export const DEFAULT_TIME_RANGE_SECONDS = 86400;
const ALERTS_EVENTS_STREAMS = ['000000000000000000000003', '000000000000000000000002'];

const messagesTodayLink = (timeRange: RelativeTimeRangeWithEnd) =>
  new URI(Routes.SEARCH).addQuery({ q: '', ...timeRangeToQueryParameter(timeRange) }).toString();

const alertsOrEventsLink = (timeRange: RelativeTimeRangeWithEnd, alert: boolean) =>
  new URI(Routes.ALERTS.LIST)
    .addQuery('page', 1)
    .addQuery('filters', [
      ...nonInfoPriorities.map((priority) => `priority=${priority}`),
      `timestamp=${serializeTimeRange(timeRange)}`,
      `alert=${alert}`,
    ])
    .toString();

const numberWidget = ({
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
}) => ({
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

const topSourcesWidget = (timeRange: RelativeTimeRangeWithEnd) => ({
  title: 'Top 5 Sources',
  widget: AggregationWidget.builder()
    .id(generateId())
    .timerange(timeRange)
    .query(createElasticsearchQueryString(''))
    .config(
      AggregationWidgetConfig.builder()
        .rowPivots([pivotForField(TIMESTAMP_FIELD, new FieldType('date', [], []))])
        .columnPivots([Pivot.createValues(['source'], { limit: 5, skip_empty_values: false, other_bucket: false })])
        .series([Series.forFunction('count()')])
        .visualization('area')
        .visualizationConfig(AreaVisualizationConfig.create('spline'))
        .rollup(false)
        .build(),
    )
    .build(),
});

const buildViewState = (permittedAlertsEventsStreams: Array<string>, timeRange: RelativeTimeRangeWithEnd) => {
  const messages = numberWidget({ title: 'Messages Today', timeRange, link: messagesTodayLink(timeRange) });
  const sources = topSourcesWidget(timeRange);

  if (permittedAlertsEventsStreams.length === 0) {
    const entries = [messages, sources];

    return ViewState.create()
      .toBuilder()
      .titles({ widget: Object.fromEntries(entries.map(({ widget, title }) => [widget.id, title])) })
      .widgets(entries.map(({ widget }) => widget))
      .widgetPositions({
        [messages.widget.id]: new WidgetPosition(1, 1, 3, 4),
        [sources.widget.id]: new WidgetPosition(5, 1, 3, 8),
      })
      .build();
  }

  const numberWidgets = [
    messages,
    numberWidget({
      title: 'Alerts Today',
      timeRange,
      link: alertsOrEventsLink(timeRange, true),
      queryString: 'alert:true',
      streams: permittedAlertsEventsStreams,
    }),
    numberWidget({
      title: 'Events Today',
      timeRange,
      link: alertsOrEventsLink(timeRange, false),
      queryString: 'alert:false',
      streams: permittedAlertsEventsStreams,
    }),
  ];
  const entries = [...numberWidgets, sources];

  return ViewState.create()
    .toBuilder()
    .titles({ widget: Object.fromEntries(entries.map(({ widget, title }) => [widget.id, title])) })
    .widgets(entries.map(({ widget }) => widget))
    .widgetPositions({
      ...Object.fromEntries(
        numberWidgets.map(({ widget }, index) => [widget.id, new WidgetPosition(1 + index * 4, 1, 1.6, 4)]),
      ),
      [sources.widget.id]: new WidgetPosition(1, 2.75, 3, 12),
    })
    .build();
};

const buildView = (permittedAlertsEventsStreams: Array<string>, timeRange: RelativeTimeRangeWithEnd) => {
  const query = QueryGenerator(undefined, undefined, undefined, timeRange);
  const search = Search.create().toBuilder().queries([query]).build();
  const view = View.create()
    .toBuilder()
    .newId()
    .type(View.Type.Dashboard)
    .state({ [query.id]: buildViewState(permittedAlertsEventsStreams, timeRange) })
    .search(search)
    .build();

  return Promise.resolve(UpdateSearchForWidgets(view));
};

const useWelcomeMetricsSearch = (rangeSeconds: number = DEFAULT_TIME_RANGE_SECONDS) => {
  const { isPermitted } = usePermissions();
  const permittedAlertsEventsStreams = useMemo(
    () => ALERTS_EVENTS_STREAMS.filter((streamId) => isPermitted(`streams:read:${streamId}`)),
    [isPermitted],
  );
  const timeRange = useMemo<RelativeTimeRangeWithEnd>(() => ({ type: 'relative', from: rangeSeconds }), [rangeSeconds]);
  const viewPromise = useMemo(
    () => buildView(permittedAlertsEventsStreams, timeRange),
    [permittedAlertsEventsStreams, timeRange],
  );

  return useCreateSearch(viewPromise);
};

export default useWelcomeMetricsSearch;
