// @flow strict
import { DEFAULT_RANGE_TYPE } from 'views/Constants';

import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { QueriesActions } from 'views/stores/QueriesStore';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import View from 'views/logic/views/View';

const _getTimerange = (query = {}) => {
  const type = query.rangetype || DEFAULT_RANGE_TYPE;
  const _setRange = (condition, newRange) => {
    if (condition) {
      return newRange;
    }
    return undefined;
  };

  switch (type) {
    case 'relative':
      return _setRange(query.relative, { type, range: query.relative });
    case 'absolute':
      return _setRange((query.from || query.to), {
        type: type,
        from: query.from,
        to: query.to,
      });
    case 'keyword':
      return _setRange(query.keyword, { type, keyword: query.keyword });
    default:
      throw new Error(`Unsupported range type ${type}`);
  }
};

const _getQueryIdFromView = (view: View) => {
  if (!view.search || !view.search.queries) {
    throw new Error('Unable to extract queries from search!');
  }
  const { queries } = view.search;
  if (queries.size !== 1) {
    throw new Error('Searches must only have a single query!');
  }
  return queries.map(({ id }) => id).first();
};

const _setQueryString = (queryId, query) => {
  const queryString = query.q;
  if (!queryString) {
    return Promise.resolve();
  }
  return QueriesActions.query(queryId, queryString);
};

const _setQueryTimerange = (queryId, query) => {
  const timerange = _getTimerange(query);
  if (!timerange) {
    return Promise.resolve();
  }
  return QueriesActions.timerange(queryId, timerange);
};

const bindSearchParamsFromQuery: ViewHook = ({ query, view }) => {
  if (view.type !== View.Type.Search) {
    return Promise.resolve(true);
  }
  const queryId = _getQueryIdFromView(view);
  return _setQueryString(queryId, query).then(() => _setQueryTimerange(queryId, query)).then(() => true);
};

export default bindSearchParamsFromQuery;
