// @flow strict
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import QueryManipulationHandler from './QueryManipulationHandler';
import Query from '../queries/Query';
import type { ValueActionHandler } from './ValueActionHandler';

export default class AddToQueryHandler extends QueryManipulationHandler {
  formatNewQuery = (oldQuery: string, field: string, value: string) => {
    const fieldPredicate = `${field}:${this.escape(value)}`;

    return this.addToQuery(oldQuery, fieldPredicate);
  };

  handle: ValueActionHandler = (queryId: string, field: string, value: string) => {
    const query: Query = this.queries.get(queryId);
    const oldQuery = query.query.query_string;
    const newQuery = this.formatNewQuery(oldQuery, field, value);
    QueriesActions.query(queryId, newQuery);
  };
}
