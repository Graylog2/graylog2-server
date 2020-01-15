// @flow strict
import { DEFAULT_RANGE_TYPE } from 'views/Constants';

import { QueriesActions } from 'views/stores/QueriesStore';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import View from 'views/logic/views/View';
import { createElasticsearchQueryString } from '../logic/queries/Query';

const _getTimerange = (query = {}) => {
  const type = query.rangetype || DEFAULT_RANGE_TYPE;

  switch (type) {
    case 'relative':
      return query.relative ? { type, range: parseInt(query.relative, 10) } : undefined;
    case 'absolute':
      return (query.from || query.to)
        ? {
          type: type,
          from: query.from,
          to: query.to,
        }
        : undefined;
    case 'keyword':
      return query.keyword ? { type, keyword: query.keyword } : undefined;
    default:
      throw new Error(`Unsupported range type ${type}`);
  }
};

const bindSearchParamsFromQuery: ViewHook = ({ query, view }) => {
  if (view.type !== View.Type.Search) {
    return Promise.resolve(true);
  }
  const { q: queryString } = query;
  const timerange = _getTimerange(query);

  if (!queryString && !timerange) {
    return Promise.resolve(true);
  }

  const { queries } = view.search;
  if (queries.size !== 1) {
    throw new Error('Searches must only have a single query!');
  }
  const firstQuery = queries.first();
  let queryBuilder = firstQuery.toBuilder();
  if (queryString !== undefined) {
    queryBuilder = queryBuilder.query(createElasticsearchQueryString(queryString));
  }
  if (timerange) {
    queryBuilder = queryBuilder.timerange(timerange);
  }

  return QueriesActions.update(firstQuery.id, queryBuilder.build());
};

export default bindSearchParamsFromQuery;
