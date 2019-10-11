// @flow strict
import * as Immutable from 'immutable';

import mockAction from 'helpers/mocking/MockAction';
import { QueriesActions, QueriesStore } from 'views/stores/QueriesStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Query from 'views/logic/queries/Query';
import AddToQueryHandler from './AddToQueryHandler';
import View from '../views/View';

jest.mock('views/stores/QueriesStore', () => ({ QueriesStore: {}, QueriesActions: {} }));

describe('AddToQueryHandler', () => {
  const view = View.create().toBuilder().type(View.Type.Search).build();
  let queriesStoreListen;
  let queries;
  beforeEach(() => {
    QueriesStore.listen = jest.fn((cb) => { queriesStoreListen = cb; });
    QueriesStore.getInitialState = jest.fn(() => queries);
    QueriesActions.query = mockAction(jest.fn(() => Promise.resolve()));
  });
  it('formats date field for ES', () => {
    const query = Query.builder()
      .query({ type: 'elasticsearch', query_string: '' })
      .build();
    queries = Immutable.Map({ queryId: query });

    const addToQueryHandler = new AddToQueryHandler();

    return addToQueryHandler.handle({
      queryId: 'queryId',
      field: 'timestamp',
      value: '2019-01-17T11:00:09.025Z',
      type: new FieldType('date', [], []),
      contexts: { view },
    })
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

    return addToQueryHandler.handle({ queryId: 'anotherQueryId', field: 'bar', value: 42, type: new FieldType('keyword', [], []), contexts: { view } })
      .then(() => {
        expect(QueriesActions.query).toHaveBeenCalledWith('anotherQueryId', 'foo:23 AND bar:42');
      });
  });
});
