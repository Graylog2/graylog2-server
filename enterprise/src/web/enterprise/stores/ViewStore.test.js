import Immutable from 'immutable';

import { ViewActions } from './ViewStore';

describe('ViewStore', () => {
  it('.load should select first query if activeQuery is not set', () => {
    const view = {
      search: {
        queries: Immutable.List([{ id: 'firstQueryId' }]),
      },
    };
    return ViewActions.load(view).then((state) => {
      expect(state.activeQuery).toBe('firstQueryId');
    });
  });
  it('.load should select activeQuery if it is set and present in view', () => {
    ViewActions.selectQuery('secondQueryId');
    const view = {
      search: {
        queries: Immutable.List([{ id: 'firstQueryId' }, { id: 'secondQueryId' }]),
      },
    };
    return ViewActions.load(view).then((state) => {
      expect(state.activeQuery).toBe('secondQueryId');
    });
  });
  it('.load should select first query if activeQuery is set but not present in view', () => {
    ViewActions.selectQuery('nonExistingQueryId');
    const view = {
      search: {
        queries: Immutable.List([{ id: 'firstQueryId' }, { id: 'secondQueryId' }]),
      },
    };
    return ViewActions.load(view).then((state) => {
      expect(state.activeQuery).toBe('firstQueryId');
    });
  });
});
