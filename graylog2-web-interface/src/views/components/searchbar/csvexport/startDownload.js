// @flow strict
import { Set } from 'immutable';

import { type ExportPayload } from 'util/MessagesExportUtils';
import StringUtils from 'util/StringUtils';

import Query from 'views/logic/queries/Query';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

const startDownload = (
  downloadFile: (payload: ExportPayload, searchQueries: Set<Query>, searchType: ?any, searchId: string, filename: string) => Promise<void>,
  view: View,
  executionState: SearchExecutionState,
  selectedWidget: ?Widget,
  selectedFields: { field: string }[],
  selectedSort: SortConfig[],
  limit: ?number,
) => {
  const payload: ExportPayload = {
    execution_state: executionState,
    fields_in_order: selectedFields.map((field) => field.field),
    sort: selectedSort.map((sortConfig) => new MessageSortConfig(sortConfig.field, sortConfig.direction)),
    limit,
  };
  let filename = 'search-result';
  let searchType;

  if (selectedWidget) {
    const widgetTitle = view.getWidgetTitleByWidget(selectedWidget);
    filename = `${widgetTitle}-${filename}`;
    searchType = view.getSearchTypeByWidgetId(selectedWidget.id);
  } else {
    const viewTitle = view.title || `Untitled ${ViewTypeLabel({ type: view.type, capitalize: true })}`;
    filename = `${viewTitle}-${filename}`;
  }
  const filenameWithoutSpaces = StringUtils.replaceSpaces(filename, '-');

  return downloadFile(payload, view.search.queries, searchType, view.search.id, filenameWithoutSpaces);
};

export default startDownload;
