import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import { QueriesActions } from 'views/stores/QueriesStore';
import QueryManipulationHandler from './QueryManipulationHandler';
import type { ActionHandler } from '../../components/actions/ActionHandler';

export default class ExcludeFromQueryHandler extends QueryManipulationHandler {
  formatNewQuery = (oldQuery, field, value) => {
    const fieldPredicate = `NOT ${field}:${escape(value)}`;

    return addToQuery(oldQuery, fieldPredicate);
  };

  handle: ActionHandler = ({ queryId, field, value }) => {
    const query = this.queries.get(queryId);
    const oldQuery = query.query.query_string;
    const newQuery = this.formatNewQuery(oldQuery, field, value);
    QueriesActions.query(queryId, newQuery);
  };
}
