import uuid from 'uuid/v4';
import Query from './Query';

export default (id = uuid()) => {
  return new Query(id, { type: 'elasticsearch', query_string: '*' }, { type: 'relative', range: 300 }, undefined, []);
};
