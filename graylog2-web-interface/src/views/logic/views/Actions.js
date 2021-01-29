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
import history from 'util/History';
import Routes from 'routing/Routes';
import View from 'views/logic/views/View';

export const loadNewView = () => history.push(`${Routes.SEARCH}/new`);

export const loadNewSearch = loadNewView;

export const loadNewViewForStream = (streamId: string) => history.push(`${Routes.stream_search(streamId)}/new`);

export const loadView = (viewId: string) => history.push(`${Routes.SEARCH}/${viewId}`);

export const loadDashboard = (dashboardId: string) => history.push(Routes.pluginRoute('DASHBOARDS_VIEWID')(dashboardId));

export const loadAsDashboard = (view: View) => history.push({
  pathname: Routes.pluginRoute('DASHBOARDS_NEW'),
  state: {
    view,
  },
});
