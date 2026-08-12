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

import generateId from 'logic/generateId';
import usePermissions from 'hooks/usePermissions';
import useCreateSearch from 'views/hooks/useCreateSearch';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import Search from 'views/logic/search/Search';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
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

const numberWidget = ({
  title,
  timeRange,
  queryString = '',
  streams = [],
}: {
  title: string;
  timeRange: RelativeTimeRangeWithEnd;
  queryString?: string;
  streams?: Array<string>;
}) => ({
  title,
  widget: AggregationWidget.builder()
    .id(generateId())
    .timerange(timeRange)
    .query(createElasticsearchQueryString(queryString))
    .streams(streams)
    .config(
      AggregationWidgetConfig.builder()
        .series([Series.forFunction('count()')])
        .visualization('numeric')
        .visualizationConfig(NumberVisualizationConfig.create(true, 'NEUTRAL'))
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
        .columnPivots([Pivot.createValues(['source'], { limit: 5, skip_empty_values: false })])
        .series([Series.forFunction('count()')])
        .visualization('area')
        .visualizationConfig(AreaVisualizationConfig.create('spline'))
        .rollup(false)
        .build(),
    )
    .build(),
});

const buildViewState = (canViewAlertsEventsStreams: boolean, timeRange: RelativeTimeRangeWithEnd) => {
  const messages = numberWidget({ title: 'Messages Today', timeRange });
  const sources = topSourcesWidget(timeRange);

  if (!canViewAlertsEventsStreams) {
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
    numberWidget({ title: 'Alerts Today', timeRange, queryString: 'alert:true', streams: ALERTS_EVENTS_STREAMS }),
    numberWidget({ title: 'Events Today', timeRange, queryString: 'alert:false', streams: ALERTS_EVENTS_STREAMS }),
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

const buildView = (canViewAlertsEventsStreams: boolean, timeRange: RelativeTimeRangeWithEnd) => {
  const query = QueryGenerator(undefined, undefined, undefined, timeRange);
  const search = Search.create().toBuilder().queries([query]).build();
  const view = View.create()
    .toBuilder()
    .newId()
    .type(View.Type.Dashboard)
    .state({ [query.id]: buildViewState(canViewAlertsEventsStreams, timeRange) })
    .search(search)
    .build();

  return Promise.resolve(UpdateSearchForWidgets(view));
};

const useWelcomeMetricsSearch = (rangeSeconds: number = DEFAULT_TIME_RANGE_SECONDS) => {
  const { isPermitted } = usePermissions();
  const canViewAlertsEventsStreams = ALERTS_EVENTS_STREAMS.every((streamId) => isPermitted(`streams:read:${streamId}`));
  const timeRange = useMemo<RelativeTimeRangeWithEnd>(() => ({ type: 'relative', from: rangeSeconds }), [rangeSeconds]);
  const viewPromise = useMemo(
    () => buildView(canViewAlertsEventsStreams, timeRange),
    [canViewAlertsEventsStreams, timeRange],
  );

  return useCreateSearch(viewPromise);
};

export default useWelcomeMetricsSearch;
