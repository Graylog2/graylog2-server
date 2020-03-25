// @flow strict
import { flatten } from 'lodash';
import { Set } from 'immutable';

import { exportAllMessages, exportSearchTypeMessages, type ExportPayload } from 'util/MessagesExportUtils';

import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import Query from 'views/logic/queries/Query';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

const _exportOnDashboard = (defaultExportPayload: ExportPayload, searchType: any, searchId: string) => {
  if (!searchType) {
    throw new Error('CSV exports on a dashboard require a selected widget!');
  }
  exportSearchTypeMessages(defaultExportPayload, searchId, searchType.id);
};

const _exportOnSearchPage = (defaultExportPayload: ExportPayload, searchQueries: Set<Query>, searchType: ?any, searchId: string) => {
  if (searchQueries.size !== 1) {
    throw new Error('Searches must only have a single query!');
  }
  const firstQuery = searchQueries.first();
  if (firstQuery) {
    if (searchType) {
      exportSearchTypeMessages(defaultExportPayload, searchId, searchType.id);
    } else {
      const { query, timerange } = firstQuery;
      const streams = firstQuery.filter ? firstQuery.filter.get('filters').filter(filter => filter.get('type') === 'stream').map(filter => filter.get('id')).toArray() : [];
      const exportPayload = {
        ...defaultExportPayload,
        timerange,
        query_string: query,
        streams,
      };
      exportAllMessages(exportPayload);
    }
  }
};

const startDownload = (view: View, selectedWidget: ?Widget, selectedFields: { field: string }[], selectedSort: SortConfig[]) => {
  let searchType;
  const defaultExportPayload = {
    fields_in_order: selectedFields.map(field => field.field),
    sort: selectedSort.map(sortConfig => new MessageSortConfig(sortConfig.field, sortConfig.direction)),
  };

  if (selectedWidget) {
    const widgetMapping = view.state.map(state => state.widgetMapping).flatten(true);
    const searchTypeId = widgetMapping.get(selectedWidget.id).first();
    const searchTypes = flatten(view.search.queries.map(query => query.searchTypes).toArray());
    searchType = searchTypes.find(entry => entry && entry.id && entry.id === searchTypeId);
  }

  if (view.type === View.Type.Dashboard) {
    _exportOnDashboard(defaultExportPayload, searchType, view.search.id);
  }

  if (view.type === View.Type.Search) {
    _exportOnSearchPage(defaultExportPayload, view.search.queries, searchType, view.search.id);
  }
};

export default startDownload;
