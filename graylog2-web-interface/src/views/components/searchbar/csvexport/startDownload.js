// @flow strict
import { Set } from 'immutable';

import { type ExportPayload } from 'util/MessagesExportUtils';
import StringUtils from 'util/StringUtils';

import Query from 'views/logic/queries/Query';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

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
  downloadFile: (payload: ExportPayload, searchQueries: Set<Query>, searchType: ?any, searchId: string, filename: string) => Promise<void>,
  view: View,
  executionState: SearchExecutionState,
  selectedWidget: ?Widget,
  selectedFields: { field: string }[],
  limit: ?number,
) => {
  const payload: ExportPayload = {
    execution_state: executionState,
    fields_in_order: selectedFields.map((field) => field.field),
    limit,
  };
  const searchType = selectedWidget ? view.getSearchTypeByWidgetId(selectedWidget.id) : undefined;
  const filename = getFilename(view, selectedWidget);

  return downloadFile(payload, view.search.queries, searchType, view.search.id, filename);
};

export default startDownload;
