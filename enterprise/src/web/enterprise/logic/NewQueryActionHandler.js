import { QueriesActions } from '../stores/QueriesStore';
import QueryGenerator from './queries/QueryGenerator';
import ViewStateGenerator from './views/ViewStateGenerator';

export default () => {
  const query = QueryGenerator();
  const state = ViewStateGenerator();
  return QueriesActions.create(query, state);
};