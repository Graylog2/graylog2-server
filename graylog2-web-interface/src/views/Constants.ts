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
import chroma from 'chroma-js';

import type { TimeRange, RelativeTimeRangeWithEnd, RelativeTimeRange } from 'views/logic/queries/Query';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import type { ArrayElement } from 'views/types';
import type { AutoTimeConfig } from 'views/logic/aggregationbuilder/Pivot';

export type SearchBarFormValues = {
  timerange: TimeRange,
  streams: Array<string>,
  streamCategories: Array<string>,
  queryString: string,
};

export const FULL_MESSAGE_FIELD = 'full_message';
export const TIMESTAMP_FIELD = 'timestamp';
export const MESSAGE_FIELD = 'message';
export const SOURCE_FIELD = 'source';

export const STREAMS_FIELD = 'streams';

export const GL2_SECOND_SORT_FIELD = 'gl2_second_sort_field';
export const FILTERED_FIELDS = [
  // ElasticSearch fields.
  '_id',
  '_ttl',
  '_source',
  '_all',
  '_index',
  '_type',
  '_score',

  // Our reserved fields.
  'gl2_accounted_message_size',
  'gl2_processing_error',
  'gl2_processing_duration_ms',
  'gl2_processing_timestamp',
  'gl2_receive_timestamp',
  'gl2_remote_hostname',
  'gl2_remote_ip',
  'gl2_remote_port',
  'gl2_source_collector',
  'gl2_source_collector_input',
  'gl2_source_input',
  'gl2_source_node',
  'gl2_message_id',
  STREAMS_FIELD,
  // TODO Due to be removed in Graylog 3.x
  'gl2_source_radio',
  'gl2_source_radio_input',
];

export const RESERVED_FIELDS = [
  FULL_MESSAGE_FIELD,
  MESSAGE_FIELD,
  TIMESTAMP_FIELD,
  SOURCE_FIELD,
  GL2_SECOND_SORT_FIELD,
];

export const MISSING_BUCKET_NAME = '(Empty Value)';
export const DEFAULT_PIVOT_LIMIT = 15;

export const DEFAULT_PIVOT_INTERVAL: AutoTimeConfig = {
  type: 'auto',
  scaling: 1.0,
};

export const DEFAULT_MESSAGE_FIELDS = [TIMESTAMP_FIELD, SOURCE_FIELD];

export const RELATIVE_ALL_TIME = 0; // value for time range `range` property, which represents all time
export const ALL_MESSAGES_TIMERANGE: RelativeTimeRange = { type: 'relative', range: RELATIVE_ALL_TIME };
export const Messages = {
  DEFAULT_LIMIT: 150,
};

export const DEFAULT_RANGE_TYPE = 'relative';
export const DEFAULT_RELATIVE_FROM = 300;
export const DEFAULT_RELATIVE_TO = DEFAULT_RELATIVE_FROM - 60;
export const DEFAULT_TIMERANGE: RelativeTimeRangeWithEnd = { type: DEFAULT_RANGE_TYPE, from: DEFAULT_RELATIVE_FROM };

export const NO_TIMERANGE_OVERRIDE = {};

export const DEFAULT_HIGHLIGHT_COLOR = StaticColor.create('#ffec3d');
export const DEFAULT_CUSTOM_HIGHLIGHT_RANGE = chroma.scale(['lightyellow', 'lightgreen', 'lightblue', 'red'])
  .mode('lch')
  .colors(40);

export const DEFAULT_INTERPOLATION = 'linear';
export const interpolationTypes = ['linear', 'step-after', 'spline'] as const;
export type InterpolationType = ArrayElement<typeof interpolationTypes>;

export const percentileOptions = [25.0, 50.0, 75.0, 90.0, 95.0, 99.0].map((value) => ({ label: value, value }));
export const percentageStrategyOptions = [
  { label: 'Document Count', value: 'COUNT' },
  { label: 'Field Sum', value: 'SUM' },
];

export const TimeUnits = {
  seconds: 'Seconds',
  minutes: 'Minutes',
  hours: 'Hours',
  days: 'Days',
  weeks: 'Weeks',
  months: 'Months',
};

export const RELATIVE_RANGE_TYPES = [
  {
    type: 'seconds',
    label: 'Seconds',
  }, {
    type: 'minutes',
    label: 'Minutes',
  }, {
    type: 'hours',
    label: 'Hours',
  }, {
    type: 'days',
    label: 'Days',
  },
] as const;

export type TimeUnit = keyof typeof TimeUnits;

export const viewsPath = '/views';
export const showViewsPath = `${viewsPath}/:viewId`;

export const searchPath = '/search';
export const newSearchPath = `${searchPath}/new`;
export const showSearchPath = `${searchPath}/:viewId`;

export const dashboardsPath = '/dashboards';
export const newDashboardsPath = `${dashboardsPath}/new`;
export const dashboardsTvPath = `${dashboardsPath}/tv/:viewId`;
export const showDashboardsPath = `${dashboardsPath}/:viewId`;

export const extendedSearchPath = '/extendedsearch';

export const availableTimeRangeTypes = [
  { type: 'relative' as const, name: 'Relative' },
  { type: 'absolute' as const, name: 'Absolute' },
  { type: 'keyword' as const, name: 'Keyword' },
];

export const VISUALIZATION_TABLE_HEADER_HEIGHT = 28;

export const keySeparator = '\u2E31';
export const humanSeparator = '-';
