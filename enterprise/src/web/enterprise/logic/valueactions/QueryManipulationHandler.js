// @flow strict
import * as Immutable from 'immutable';
import { QueriesStore } from 'enterprise/stores/QueriesStore';
import Query from '../queries/Query';

export default class QueryManipulationHandler {
  queries: Immutable.Set<Query>;

  constructor() {
    this.queries = QueriesStore.getInitialState();
    QueriesStore.listen((queries) => { this.queries = queries; });
  }
}
