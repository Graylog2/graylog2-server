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
import { stringify } from 'qs';

import Routes from 'routing/Routes';
import type View from 'views/logic/views/View';
import type { HistoryFunction } from 'routing/useHistory';

export const loadNewView = (history: HistoryFunction) => history.push(`${Routes.SEARCH}/new`);

export const loadNewSearch = loadNewView;

export const loadNewViewForStream = (history: HistoryFunction, streamId: string) => history.push(`${Routes.stream_search(streamId)}/new`);

export const loadView = (history: HistoryFunction, viewId: string) => history.push(`${Routes.SEARCH}/${viewId}`);

export const loadDashboard = (history: HistoryFunction, dashboardId: string, initialPage?: string) => history.push(
  `${Routes.pluginRoute('DASHBOARDS_VIEWID')(dashboardId)}${initialPage ? `?${stringify({ page: initialPage })}` : ''}`,
);

export const loadAsDashboard = (history: HistoryFunction, view: View) => history.pushWithState(
  Routes.pluginRoute('DASHBOARDS_NEW'),
  {
    view,
  },
);
