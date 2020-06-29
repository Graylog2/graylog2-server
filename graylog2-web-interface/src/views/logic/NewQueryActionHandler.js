// @flow strict
import { ViewStore } from 'views/stores/ViewStore';

import QueryGenerator from './queries/QueryGenerator';
import ViewStateGenerator from './views/ViewStateGenerator';

import { QueriesActions } from '../stores/QueriesStore';

export default async () => {
  const { view } = ViewStore.getInitialState();
  const query = QueryGenerator();
  const state = await ViewStateGenerator(view.type);

  return QueriesActions.create(query, state).then(() => query);
};
