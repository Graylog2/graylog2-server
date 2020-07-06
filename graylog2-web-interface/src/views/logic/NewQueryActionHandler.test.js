// @flow strict
import asMock from 'helpers/mocking/AsMock';

import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewStore } from 'views/stores/ViewStore';

import NewQueryActionHandler from './NewQueryActionHandler';
import View from './views/View';

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: {
    getInitialState: jest.fn(),
  },
}));

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    create: jest.fn(() => Promise.resolve()),
  },
}));

describe('NewQueryActionHandler', () => {
  beforeEach(() => {
    asMock(ViewStore.getInitialState).mockReturnValue({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
    });
  });

  it('creates a new query', () => NewQueryActionHandler()
    .then(() => expect(QueriesActions.create).toHaveBeenCalled()));
});
