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
// @flow strict
import chroma from 'chroma-js';

export const TIMESTAMP_FIELD = 'timestamp';
export const DEFAULT_MESSAGE_FIELDS = [TIMESTAMP_FIELD, 'source'];
export const Messages = {
  DEFAULT_LIMIT: 150,
};

export const DEFAULT_RANGE_TYPE = 'relative';
export const DEFAULT_TIMERANGE = { type: DEFAULT_RANGE_TYPE, range: 300 };

export const DEFAULT_HIGHLIGHT_COLOR = '#ffec3d';
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

export type TimeUnit = $Keys<typeof TimeUnits>;

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
  { type: 'relative', name: 'Relative' },
  { type: 'absolute', name: 'Absolute' },
  { type: 'keyword', name: 'Keyword' },
];
