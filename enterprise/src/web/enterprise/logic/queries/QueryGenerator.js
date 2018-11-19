// @flow
import uuid from 'uuid/v4';
import Query from './Query';
import type { QueryId } from './Query';

export default (id: QueryId = uuid()): Query => {
  return new Query(id, { type: 'elasticsearch', query_string: '*' }, { type: 'relative', range: 300 }, undefined, []);
};
