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
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import FieldType from 'views/logic/fieldtypes/FieldType';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { TIMESTAMP_FIELD } from 'views/Constants';

import useWelcomeSearch, {
  numberWidget,
  messagesTodayLink,
  alertsOrEventsLink,
  type WelcomeSearchWidgetEntry,
} from './useWelcomeSearch';
import useMetricsTimeRange from './useMetricsTimeRange';

const ALERTS_EVENTS_STREAMS = ['000000000000000000000003', '000000000000000000000002'];

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

const buildEntries = (
  permittedAlertsEventsStreams: Array<string>,
  timeRange: RelativeTimeRangeWithEnd,
  topSourcesOnly: boolean,
): Array<WelcomeSearchWidgetEntry> => {
  const sources = topSourcesWidget(timeRange);

  if (topSourcesOnly) {
    return [{ ...sources, position: new WidgetPosition(1, 1, 3, 12) }];
  }

  const messages = numberWidget({ title: 'Messages Today', timeRange, link: messagesTodayLink(timeRange) });

  if (permittedAlertsEventsStreams.length === 0) {
    return [
      { ...messages, position: new WidgetPosition(1, 1, 3, 4) },
      { ...sources, position: new WidgetPosition(5, 1, 3, 8) },
    ];
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

  return [
    ...numberWidgets.map(({ title, widget }, index) => ({
      title,
      widget,
      position: new WidgetPosition(1 + index * 4, 1, 1.6, 4),
    })),
    { ...sources, position: new WidgetPosition(1, 2.75, 3, 12) },
  ];
};

const useWelcomeMetricsSearch = (topSourcesOnly: boolean = false) => {
  const rangeSeconds = useMetricsTimeRange();
  const { isPermitted } = usePermissions();
  const permittedAlertsEventsStreams = useMemo(
    () => ALERTS_EVENTS_STREAMS.filter((streamId) => isPermitted(`streams:read:${streamId}`)),
    [isPermitted],
  );
  const timeRange = useMemo<RelativeTimeRangeWithEnd>(() => ({ type: 'relative', from: rangeSeconds }), [rangeSeconds]);

  return useWelcomeSearch(buildEntries(permittedAlertsEventsStreams, timeRange, topSourcesOnly), timeRange);
};

export default useWelcomeMetricsSearch;
