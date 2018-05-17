import { QueriesActions } from '../stores/QueriesStore';

export default (queryId) => {
  QueriesActions.remove(queryId);
};