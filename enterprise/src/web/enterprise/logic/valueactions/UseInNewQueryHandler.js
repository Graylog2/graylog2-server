// @flow strict
import { QueriesActions } from 'enterprise/actions/QueriesActions';
import QueryGenerator from 'enterprise/logic/queries/QueryGenerator';
import ViewStateGenerator from 'enterprise/logic/views/ViewStateGenerator';
import Query from 'enterprise/logic/queries/Query';
import ViewState from 'enterprise/logic/views/ViewState';
import { escape } from 'enterprise/logic/queries/QueryHelper';
import type { ValueActionHandlerWithContext } from './ValueActionHandler';

const UseInNewQueryHandler: ValueActionHandlerWithContext = (queryId: string, field: string, value: string) => {
  const query: Query = QueryGenerator().toBuilder()
    .query({ type: 'elasticsearch', query_string: `${field}:${escape(value)}` })
    .build();
  const state: ViewState = ViewStateGenerator();
  return QueriesActions.create(query, state);
};

export default UseInNewQueryHandler;
