// @flow strict
import { Set } from 'immutable';

import { exportSearchMessages, exportSearchTypeMessages, type ExportPayload } from 'util/MessagesExportUtils';
import StringUtils from 'util/StringUtils';

import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import Query from 'views/logic/queries/Query';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

const _exportOnDashboard = (defaultExportPayload: ExportPayload, searchType: any, searchId: string, filename: string) => {
  if (!searchType) {
    throw new Error('CSV exports on a dashboard require a selected widget!');
  }
  return exportSearchTypeMessages(defaultExportPayload, searchId, searchType.id, filename);
};

const _exportOnSearchPage = (defaultExportPayload: ExportPayload, searchQueries: Set<Query>, searchType: ?any, searchId: string, filename: string) => {
  if (searchQueries.size !== 1) {
    throw new Error('Searches must only have a single query!');
  }
  if (searchType) {
    return exportSearchTypeMessages(defaultExportPayload, searchId, searchType.id, filename);
  }
  return exportSearchMessages(defaultExportPayload, searchId, filename);
};

const startDownload = (view: View, executionState: SearchExecutionState, selectedWidget: ?Widget, selectedFields: { field: string }[], selectedSort: SortConfig[], limit: ?number) => {
  const defaultExportPayload = {
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

  if (view.type === View.Type.Dashboard) {
    return _exportOnDashboard(defaultExportPayload, searchType, view.search.id, filenameWithoutSpaces);
  }

  if (view.type === View.Type.Search) {
    return _exportOnSearchPage(defaultExportPayload, view.search.queries, searchType, view.search.id, filenameWithoutSpaces);
  }

  throw new Error(`Message export not supported for defined view type ${view.type}`);
};

export default startDownload;
