// @flow
import uuid from 'uuid/v4';
import Query from './Query';
import type { QueryId } from './Query';

export default (id: QueryId = uuid()): Query => {
  return Query.builder()
    .id(id)
    .query({ type: 'elasticsearch', query_string: '*' })
    .timerange({ type: 'relative', range: 300 })
    .build();
};
