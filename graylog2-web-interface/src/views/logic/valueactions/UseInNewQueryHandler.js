// @flow strict
import { QueriesActions } from 'views/actions/QueriesActions';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import ViewStateGenerator from 'views/logic/views/ViewStateGenerator';
import Query from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import { escape } from 'views/logic/queries/QueryHelper';
import type { ValueActionHandler } from './ValueActionHandler';
import View from '../views/View';
import type { ActionHandlerCondition } from '../../components/actions/ActionHandler';

const UseInNewQueryHandler: ValueActionHandler = async ({ field, value, contexts: { view } }) => {
  const query: Query = QueryGenerator().toBuilder()
    .query({ type: 'elasticsearch', query_string: `${field}:${escape(value)}` })
    .build();
  const state: ViewState = await ViewStateGenerator(view.type);
  return QueriesActions.create(query, state);
};

const condition: ActionHandlerCondition = ({ contexts: { view } }) => !view || view.type !== View.Type.Dashboard;

UseInNewQueryHandler.isEnabled = condition;

export default UseInNewQueryHandler;
