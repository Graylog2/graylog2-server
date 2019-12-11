// @flow strict
import { ViewStore } from 'views/stores/ViewStore';
import { QueriesActions } from '../stores/QueriesStore';
import QueryGenerator from './queries/QueryGenerator';
import ViewStateGenerator from './views/ViewStateGenerator';

export default async () => {
  const { view } = ViewStore.getInitialState();
  const query = QueryGenerator();
  const state = await ViewStateGenerator(view.type);
  return QueriesActions.create(query, state).then(() => query);
};
