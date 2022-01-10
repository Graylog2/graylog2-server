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
import type { Set } from 'immutable';

import type { ExportPayload } from 'util/MessagesExportUtils';
import StringUtils from 'util/StringUtils';
import type Query from 'views/logic/queries/Query';
import type View from 'views/logic/views/View';
import type Widget from 'views/logic/widgets/Widget';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchType } from 'views/logic/queries/SearchType';
import type { ExportSettings } from 'views/components/ExportSettingsContext';

const getFilename = (view, selectedWidget) => {
  let filename = 'search-result';

  if (selectedWidget) {
    const widgetTitle = view.getWidgetTitleByWidget(selectedWidget);

    filename = `${widgetTitle}-${filename}`;
  } else {
    const viewTitle = view.title || `Untitled ${ViewTypeLabel({ type: view.type, capitalize: true })}`;

    filename = `${viewTitle}-${filename}`;
  }

  return StringUtils.replaceSpaces(filename, '-');
};

const startDownload = (
  format: string,
  downloadFile: (format: string, payload: ExportPayload, searchQueries: Set<Query>, searchType: SearchType | undefined | null, searchId: string, filename: string) => Promise<void>,
  view: View,
  executionState: SearchExecutionState,
  selectedWidget: Widget | undefined | null,
  selectedFields: { field: string }[],
  limit: number | undefined | null,
  customSettings: ExportSettings,
) => {
  const payload: ExportPayload = {
    execution_state: executionState,
    fields_in_order: selectedFields.map((field) => field.field),
    limit,
    ...customSettings,
  };
  const searchType: SearchType | undefined | null = selectedWidget ? view.getSearchTypeByWidgetId(selectedWidget.id) : undefined;
  const filename = getFilename(view, selectedWidget);

  return downloadFile(format, payload, view.search.queries, searchType, view.search.id, filename);
};

export default startDownload;
