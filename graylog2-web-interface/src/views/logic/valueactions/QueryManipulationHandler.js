// @flow strict
import * as Immutable from 'immutable';
import { QueriesStore } from 'views/stores/QueriesStore';
import Query from '../queries/Query';
import type { QueryId } from '../queries/Query';

export default class QueryManipulationHandler {
  queries: Immutable.OrderedMap<QueryId, Query>;

  constructor() {
    this.queries = QueriesStore.getInitialState();
    QueriesStore.listen((queries) => { this.queries = queries; });
  }
}
