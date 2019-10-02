// @flow strict
import chroma from 'chroma-js';

export const TIMESTAMP_FIELD = 'timestamp';
export const DEFAULT_MESSAGE_FIELDS = [TIMESTAMP_FIELD, 'source'];
export const Messages = {
  DEFAULT_LIMIT: 150,
};

export const DEFAULT_TIMERANGE = { type: 'relative', range: 300 };

export const DEFAULT_HIGHLIGHT_COLOR = '#ffec3d';
export const DEFAULT_CUSTOM_HIGHLIGHT_RANGE = chroma.scale(['lightyellow', 'lightgreen', 'lightblue', 'red']).mode('lch').colors(40);

export const dashboardsPath = '/dashboards';
export const newDashboardsPath = '/dashboards/new';
export const extendedSearchPath = '/extendedsearch';
export const viewsPath = '/views';
export const showViewsPath = `${viewsPath}/:viewId`;
export const showDashboardsPath = `${dashboardsPath}/:viewId`;
