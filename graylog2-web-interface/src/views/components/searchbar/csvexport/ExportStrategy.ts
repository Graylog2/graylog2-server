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
import { Set, List } from 'immutable';

import View, { ViewType } from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import { exportSearchMessages, exportSearchTypeMessages, ExportPayload } from 'util/MessagesExportUtils';
import Query from 'views/logic/queries/Query';
import type { SearchType } from 'views/logic/queries/SearchType';

type ExportStrategy = {
  title: string,
  shouldAllowWidgetSelection: (singleWidgetDownload: boolean, showWidgetSelection: boolean, widgets: List<Widget>) => boolean,
  shouldEnableDownload: (showWidgetSelection: boolean, selectedWidget: Widget | undefined | null, selectedFields: { field: string }[], loading: boolean) => boolean,
  shouldShowWidgetSelection: (singleWidgetDownload: boolean, selectedWidget: Widget | undefined | null, widgets: List<Widget>) => boolean,
  initialWidget: (widgets: List<Widget>, directExportWidgetId: string | undefined | null) => Widget | undefined | null,
  downloadFile: (payload: ExportPayload, searchQueries: Set<Query>, searchType: SearchType | undefined | null, searchId: string, filename: string) => Promise<void>,
};

const _getWidgetById = (widgets, id) => widgets.find((item) => item.id === id);

const _initialSearchWidget = (widgets, directExportWidgetId) => {
  if (directExportWidgetId) {
    return _getWidgetById(widgets, directExportWidgetId);
  }

  if (widgets.size === 1) {
    return widgets.first();
  }

  return null;
};

const _exportOnDashboard = (payload: ExportPayload, searchType: SearchType | undefined | null, searchId: string, filename: string) => {
  if (!searchType) {
    throw new Error('CSV exports on a dashboard require a selected widget!');
  }

  return exportSearchTypeMessages(payload, searchId, searchType.id, filename);
};

const _exportOnSearchPage = (payload: ExportPayload, searchQueries: Set<Query>, searchType: SearchType | undefined | null, searchId: string, filename: string) => {
  if (searchQueries.size !== 1) {
    throw new Error('Searches must only have a single query!');
  }

  if (searchType) {
    return exportSearchTypeMessages(payload, searchId, searchType.id, filename);
  }

  return exportSearchMessages(payload, searchId, filename);
};

const SearchExportStrategy: ExportStrategy = {
  title: 'Export all search results to CSV',
  shouldEnableDownload: (showWidgetSelection, selectedWidget, selectedFields, loading) => !loading && !showWidgetSelection && !!selectedFields && selectedFields.length > 0,
  shouldAllowWidgetSelection: (singleWidgetDownload, showWidgetSelection, widgets) => !singleWidgetDownload && !showWidgetSelection && widgets.size > 1,
  shouldShowWidgetSelection: (singleWidgetDownload, selectedWidget, widgets) => !singleWidgetDownload && !selectedWidget && widgets.size > 1,
  initialWidget: _initialSearchWidget,
  downloadFile: (payload, searchQueries, searchType, searchId, filename) => _exportOnSearchPage(payload, searchQueries, searchType, searchId, filename),
};

const DashboardExportStrategy: ExportStrategy = {
  title: 'Export message table search results to CSV',
  shouldEnableDownload: (showWidgetSelection, selectedWidget, selectedFields, loading) => !loading && !!selectedWidget && !!selectedFields && selectedFields.length > 0,
  shouldAllowWidgetSelection: (singleWidgetDownload, showWidgetSelection) => !singleWidgetDownload && !showWidgetSelection,
  shouldShowWidgetSelection: (singleWidgetDownload, selectedWidget) => !singleWidgetDownload && !selectedWidget,
  initialWidget: (widget, directExportWidgetId) => (directExportWidgetId ? _getWidgetById(widget, directExportWidgetId) : null),
  downloadFile: (payload, searchQueries, searchType, searchId, filename) => _exportOnDashboard(payload, searchType, searchId, filename),
};

const createExportStrategy = (viewType: ViewType) => {
  switch (viewType) {
    case View.Type.Dashboard:
      return DashboardExportStrategy;
    case View.Type.Search:
    default:
      return SearchExportStrategy;
  }
};

export default { createExportStrategy };
