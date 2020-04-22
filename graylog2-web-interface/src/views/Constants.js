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
export const DEFAULT_CUSTOM_HIGHLIGHT_RANGE = chroma.scale(['lightyellow', 'lightgreen', 'lightblue', 'red']).mode('lch').colors(40);

export const TimeUnits = {
  seconds: 'Seconds',
  minutes: 'Minutes',
  hours: 'Hours',
  days: 'Days',
  weeks: 'Weeks',
  months: 'Months',
};

export type TimeUnit = $Keys<typeof TimeUnits>;

export const dashboardsPath = '/dashboards';
export const viewsPath = '/views';
export const searchPath = '/search';
export const newDashboardsPath = `${dashboardsPath}/new`;
export const dashboardsTvPath = `${dashboardsPath}/tv/:viewId`;
export const extendedSearchPath = '/extendedsearch';
export const showSearchPath = `${searchPath}/:viewId`;
export const showViewsPath = `${viewsPath}/:viewId`;
export const showDashboardsPath = `${dashboardsPath}/:viewId`;

export const availableTimeRangeTypes = [
  { type: 'relative', name: 'Relative' },
  { type: 'absolute', name: 'Absolute' },
  { type: 'keyword', name: 'Keyword' },
];
