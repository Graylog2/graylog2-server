// @flow strict
import uuid from 'uuid/v4';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import Query, { createElasticsearchQueryString, filtersForQuery } from './Query';
import type { QueryId } from './Query';

export default (id: QueryId = uuid(), streamId: ?string): Query => {
  const streamIds = streamId ? [streamId] : null;
  const streamFilter = filtersForQuery(streamIds);
  return Query.builder()
    .id(id)
    .query(createElasticsearchQueryString())
    .filter(streamFilter)
    .timerange(DEFAULT_TIMERANGE)
    .build();
};
