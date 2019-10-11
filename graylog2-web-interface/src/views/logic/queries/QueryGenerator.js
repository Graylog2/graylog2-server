// @flow strict
import uuid from 'uuid/v4';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import Query from './Query';
import type { QueryId } from './Query';

export default (id: QueryId = uuid()): Query => {
  return Query.builder()
    .id(id)
    .query({ type: 'elasticsearch', query_string: '' })
    .timerange(DEFAULT_TIMERANGE)
    .build();
};
