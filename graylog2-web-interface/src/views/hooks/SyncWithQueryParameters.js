// @flow strict
import { useEffect } from 'react';
import URI from 'urijs';
import history from 'util/History';

import { ViewStore } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import { QueriesActions } from 'views/actions/QueriesActions';

const useActionListeners = (actions, callback, dependencies) => {
  useEffect(() => {
    const unsubscribes = actions.map(action => action.listen(callback));
    return () => unsubscribes.forEach(unsubscribe => unsubscribe());
  }, dependencies);
};

const extractTimerangeParams = (timerange) => {
  const { type } = timerange;
  const result = { rangetype: type };

  switch (type) {
    case 'relative': return Object.entries({ ...result, relative: timerange.range });
    case 'keyword': return Object.entries({ ...result, keyword: timerange.keyword });
    case 'absolute': return Object.entries({ ...result, from: timerange.from, to: timerange.to });
    default: return Object.entries(result);
  }
};

export const syncWithQueryParameters = (pathname: string, search: string) => {
  const query = `${pathname}${search}`;
  const { view } = ViewStore.getInitialState() || {};
  if (view && view.type === View.Type.Search) {
    const { queries } = view.search;
    if (queries.size !== 1) {
      throw new Error('Searches must only have a single query!');
    }
    const firstQuery = queries.first();
    if (firstQuery) {
      const { query: { query_string: queryString }, timerange } = firstQuery;
      const baseUri = new URI(query).setSearch('q', queryString)
        .removeQuery('from')
        .removeQuery('to')
        .removeQuery('keyword')
        .removeQuery('relative');
      const uri = extractTimerangeParams(timerange)
        .reduce((prev, [key, value]) => prev.setSearch(key, value), baseUri)
        .toString();
      if (query !== uri) {
        if (search) {
          history.push(uri);
        } else {
          history.replace(uri);
        }
      }
    }
  }
};

export const useSyncWithQueryParameters = (pathname: string, search: string) => {
  useEffect(() => syncWithQueryParameters(pathname, search), []);
  useActionListeners(
    [QueriesActions.query.completed, QueriesActions.timerange.completed],
    () => syncWithQueryParameters(pathname, search),
    [pathname, search],
  );
};
