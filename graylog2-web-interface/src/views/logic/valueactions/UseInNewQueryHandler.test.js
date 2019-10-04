// @flow strict
import asMock from 'helpers/mocking/AsMock';
import mockAction from 'helpers/mocking/MockAction';

import { QueriesActions } from 'views/actions/QueriesActions';
import UseInNewQueryHandler from './UseInNewQueryHandler';
import Query from '../queries/Query';
import FieldType from '../fieldtypes/FieldType';
import View from '../views/View';

jest.mock('views/actions/QueriesActions', () => ({ QueriesActions: {} }));
describe('UseInNewQueryHandler', () => {
  it('creates new query with generated query string', () => {
    QueriesActions.create = mockAction(jest.fn(() => Promise.resolve()));

    const view = View.create().toBuilder().type(View.Type.Dashboard).build();
    const promise = UseInNewQueryHandler({ queryId: 'queryId', field: 'foo', value: 'bar', type: FieldType.Unknown, contexts: { view } });

    return promise.then(() => {
      expect(QueriesActions.create).toHaveBeenCalled();
      const newQuery: Query = asMock(QueriesActions.create).mock.calls[0][0];

      expect(newQuery.query.type).toEqual('elasticsearch');
      expect(newQuery.query.query_string).toEqual('foo:bar');
    });
  });
});
