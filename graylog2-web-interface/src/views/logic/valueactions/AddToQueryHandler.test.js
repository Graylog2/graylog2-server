// @flow strict
import * as Immutable from 'immutable';

import { QueriesActions, QueriesStore } from 'enterprise/stores/QueriesStore';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import Query from 'enterprise/logic/queries/Query';
import AddToQueryHandler from './AddToQueryHandler';

jest.mock('enterprise/stores/QueriesStore', () => ({ QueriesStore: {}, QueriesActions: {} }));

describe('AddToQueryHandler', () => {
  let queriesStoreListen;
  let queries;
  beforeEach(() => {
    QueriesStore.listen = jest.fn((cb) => { queriesStoreListen = cb; });
    QueriesStore.getInitialState = jest.fn(() => queries);
    QueriesActions.query = jest.fn(() => Promise.resolve());
  });
  it('formats date field for ES', () => {
    const query = Query.builder()
      .query({ type: 'elasticsearch', query_string: '' })
      .build();
    queries = Immutable.Map({ queryId: query });

    const addToQueryHandler = new AddToQueryHandler();

    return addToQueryHandler.handle('queryId', 'timestamp', '2019-01-17T11:00:09.025Z', new FieldType('date', [], []))
      .then(() => {
        expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'timestamp:"2019-01-17 11:00:09.025"');
      });
  });
  it('updates query string before adding predicate', () => {
    const query = Query.builder()
      .query({ type: 'elasticsearch', query_string: '' })
      .build();
    queries = Immutable.Map({ queryId: query });

    const addToQueryHandler = new AddToQueryHandler();

    const newQuery = Query.builder()
      .query({ type: 'elasticsearch', query_string: 'foo:23' })
      .build();

    queriesStoreListen(Immutable.Map({ anotherQueryId: newQuery }));

    return addToQueryHandler.handle('anotherQueryId', 'bar', 42, new FieldType('keyword', [], []))
      .then(() => {
        expect(QueriesActions.query).toHaveBeenCalledWith('anotherQueryId', 'foo:23 AND bar:42');
      });
  });
});
