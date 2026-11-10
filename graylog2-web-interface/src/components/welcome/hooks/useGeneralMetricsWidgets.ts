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
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import FieldType from 'views/logic/fieldtypes/FieldType';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import { TIMESTAMP_FIELD } from 'views/Constants';
import {
  numberWidget,
  messagesTodayLink,
  alertsOrEventsLink,
  type WelcomeWidgetEntry,
} from 'components/welcome/welcomeWidgetBuilders';

import useMetricsTimeRange from './useMetricsTimeRange';
import usePermittedAlertsEventsStreams from './usePermittedAlertsEventsStreams';

const CHART_HEIGHT = '300px';
const NUMBER_HEIGHT = '150px';

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
): Array<WelcomeWidgetEntry> => {
  const sources = topSourcesWidget(timeRange);

  if (topSourcesOnly) {
    return [{ ...sources, columns: 12, height: CHART_HEIGHT }];
  }

  const messages = numberWidget({ title: 'Messages Today', timeRange, link: messagesTodayLink(timeRange) });

  if (permittedAlertsEventsStreams.length === 0) {
    return [
      { ...messages, columns: 4, height: CHART_HEIGHT },
      { ...sources, columns: 8, height: CHART_HEIGHT },
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
    ...numberWidgets.map((numberWidgetEntry) => ({ ...numberWidgetEntry, columns: 4, height: NUMBER_HEIGHT })),
    { ...sources, columns: 12, height: CHART_HEIGHT },
  ];
};

const useGeneralMetricsWidgets = (topSourcesOnly: boolean = false) => {
  const rangeSeconds = useMetricsTimeRange();
  const permittedAlertsEventsStreams = usePermittedAlertsEventsStreams();
  const timeRange = useMemo<RelativeTimeRangeWithEnd>(() => ({ type: 'relative', from: rangeSeconds }), [rangeSeconds]);

  // Building the entries assigns new widget ids, so the result has to be stable to avoid running the search
  // again on every render.
  return useMemo(
    () => buildEntries(permittedAlertsEventsStreams, timeRange, topSourcesOnly),
    [permittedAlertsEventsStreams, timeRange, topSourcesOnly],
  );
};

export default useGeneralMetricsWidgets;
