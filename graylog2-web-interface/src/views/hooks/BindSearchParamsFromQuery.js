// @flow strict
import { DEFAULT_RANGE_TYPE } from 'views/Constants';

import { QueriesActions } from 'views/stores/QueriesStore';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import View from 'views/logic/views/View';
import { createElasticsearchQueryString, filtersForQuery } from '../logic/queries/Query';

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

const _getStreams = (query = {}): Array<string> => {
  const rawStreams = query.streams;
  if (rawStreams === undefined || rawStreams === null) {
    return [];
  }
  return String(rawStreams).split(',')
    .map(stream => stream.trim())
    .filter(stream => (stream !== ''));
};

const bindSearchParamsFromQuery: ViewHook = ({ query, view }) => {
  if (view.type !== View.Type.Search) {
    return Promise.resolve(true);
  }
  const { q: queryString } = query;
  const timerange = _getTimerange(query);
  const streams = filtersForQuery(_getStreams(query));

  if (!queryString && !timerange && !streams) {
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
  if (streams) {
    queryBuilder = queryBuilder.filter(streams);
  }

  return QueriesActions.update(firstQuery.id, queryBuilder.build());
};

export default bindSearchParamsFromQuery;
