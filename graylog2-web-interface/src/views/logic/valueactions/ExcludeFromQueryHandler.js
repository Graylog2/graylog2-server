// @flow strict
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import type { ActionHandler } from 'views/components/actions/ActionHandler';
import QueryManipulationHandler from './QueryManipulationHandler';

export default class ExcludeFromQueryHandler extends QueryManipulationHandler {
  formatNewQuery = (oldQuery: string, field: string, value: any) => {
    const fieldPredicate = `NOT ${field}:${escape(value)}`;

    return addToQuery(oldQuery, fieldPredicate);
  };

  handle: ActionHandler = ({ queryId, field, value }) => {
    const oldQuery = this.currentQueryString(queryId);
    const newQuery = this.formatNewQuery(oldQuery, field, value);

    return this.updateQueryString(queryId, newQuery);
  };
}
