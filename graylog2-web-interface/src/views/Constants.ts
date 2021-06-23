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

import { TimeRange, RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';

export type SearchBarFormValues = {
  timerange: TimeRange,
  streams: Array<string>,
  queryString: string,
};

export const FULL_MESSAGE_FIELD = 'full_message';
export const TIMESTAMP_FIELD = 'timestamp';
export const MESSAGE_FIELD = 'message';
export const SOURCE_FIELD = 'source';

export const DEFAULT_MESSAGE_FIELDS = [TIMESTAMP_FIELD, SOURCE_FIELD];

export const RELATIVE_ALL_TIME = 0; // value for time range `range` property, which represents all time
export const Messages = {
  DEFAULT_LIMIT: 150,
};

export const DEFAULT_RANGE_TYPE = 'relative';
export const DEFAULT_RELATIVE_FROM = 300;
export const DEFAULT_RELATIVE_TO = DEFAULT_RELATIVE_FROM - 60;
export const DEFAULT_TIMERANGE: RelativeTimeRangeWithEnd = { type: DEFAULT_RANGE_TYPE, from: DEFAULT_RELATIVE_FROM };

export const DEFAULT_HIGHLIGHT_COLOR = StaticColor.create('#ffec3d');
export const DEFAULT_CUSTOM_HIGHLIGHT_RANGE = chroma.scale(['lightyellow', 'lightgreen', 'lightblue', 'red'])
  .mode('lch')
  .colors(40);

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
