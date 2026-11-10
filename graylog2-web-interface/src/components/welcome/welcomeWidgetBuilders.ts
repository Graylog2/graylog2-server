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
import URI from 'urijs';

import generateId from 'logic/generateId';
import Routes from 'routing/Routes';
import { nonInfoPriorities } from 'components/events/Constants';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import { timeRangeToQueryParameter } from 'views/logic/TimeRange';
import { serializeTimeRange } from 'components/common/EntityFilters/helpers/timeRange';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import type Widget from 'views/logic/widgets/Widget';

export const DEFAULT_TIME_RANGE_SECONDS = 86400;

export type WelcomeWidgetEntry = {
  title: string;
  widget: Widget;
  /** How many of the 12 grid columns the widget spans. */
  columns: number;
  /** CSS height, e.g. `312px`. */
  height: string;
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
