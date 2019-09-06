// @flow strict
import { QueriesActions } from 'views/actions/QueriesActions';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import ViewStateGenerator from 'views/logic/views/ViewStateGenerator';
import Query from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import { escape } from 'views/logic/queries/QueryHelper';
import type { ValueActionHandlerWithContext, ValueActionHideCondition } from './ValueActionHandler';
import View from '../views/View';
import type { ActionContexts } from '../ActionContext';
import FieldType from '../fieldtypes/FieldType';

const UseInNewQueryHandler: ValueActionHandlerWithContext = (queryId: string, field: string, value: string, type: FieldType, { view }: ActionContexts) => {
  const query: Query = QueryGenerator().toBuilder()
    .query({ type: 'elasticsearch', query_string: `${field}:${escape(value)}` })
    .build();
  const state: ViewState = ViewStateGenerator(view.type);
  return QueriesActions.create(query, state);
};

const condition: ValueActionHideCondition = ({ view }) => !view || view.type !== View.Type.Dashboard;

UseInNewQueryHandler.isEnabled = condition;

export default UseInNewQueryHandler;
