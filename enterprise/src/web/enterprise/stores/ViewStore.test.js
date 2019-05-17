import * as Immutable from 'immutable';

import ViewState from 'enterprise/logic/views/ViewState';
import SearchActions from 'enterprise/actions/SearchActions';
import Search from 'enterprise/logic/search/Search';
import Query from 'enterprise/logic/queries/Query';
import View from 'enterprise/logic/views/View';
import AggregationWidget from '../logic/aggregationbuilder/AggregationWidget';
import { ViewActions, ViewStore } from './ViewStore';
import { ViewManagementActions } from './ViewManagementStore';

jest.mock('enterprise/actions/SearchActions');
jest.mock('enterprise/logic/Widget', () => ({
  widgetDefinition: () => ({
    searchTypes: () => [],
  }),
  resultHistogram: () => ({}),
  allMessagesTable: () => ({}),
}));
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

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
  describe('maintains dirty flag:', () => {
    beforeEach(() => {
      SearchActions.create = jest.fn(s => Promise.resolve({ search: s }));
    });
    it('resets dirty flag when an existing view is updated', () => {
      const search = Search.create();
      const newView = View.builder().newId().search(search).build();
      return ViewActions.load(newView)
        .then(() => ViewActions.description('My view!'))
        .then(({ view }) => {
          const state = ViewStore.getInitialState();
          expect(state.dirty).toEqual(true);
          return ViewManagementActions.update(view);
        })
        .then(() => {
          const state = ViewStore.getInitialState();
          expect(state.dirty).toEqual(false);
        });
    });
    it('sets dirty flag to false when creating a view', () => {
      return ViewActions.create()
        .then(({ dirty }) => expect(dirty).toBeFalsy());
    });
  });

  describe('search recreation:', () => {
    let search: Search;
    let view: View;
    beforeEach(() => {
      SearchActions.create = jest.fn(s => Promise.resolve({ search: s }));

      search = Search.create()
        .toBuilder()
        .queries([Query.builder().id('firstQueryId').build()])
        .build();
      view = View.create()
        .toBuilder()
        .state(Immutable.fromJS({ firstQueryId: ViewState.create() }))
        .search(search)
        .build();
      return ViewStore.load(view);
    });
    it('should create search when view is created', (done) => {
      ViewActions.create()
        .then(() => {
          expect(SearchActions.create).toHaveBeenCalled();
          done();
        });
    });
    it('should create search when state is updated', (done) => {
      const newState = Immutable.fromJS({
        firstQueryId: ViewState.create()
          .toBuilder()
          .widgets(Immutable.fromJS([AggregationWidget.builder().build()]))
          .build(),
      });

      ViewActions.state(newState)
        .then(() => {
          expect(SearchActions.create).toHaveBeenCalled();
          done();
        });
    });
    it('should not recreate search when state is updated to identical state', (done) => {
      ViewActions.state(view.state)
        .then(() => {
          expect(SearchActions.create).not.toHaveBeenCalled();
          done();
        });
    });
    it('should create search when search is replaced', (done) => {
      const newSearch = search.toBuilder().newId().build();
      ViewActions.search(newSearch)
        .then(() => {
          expect(SearchActions.create).toHaveBeenCalled();
          done();
        });
    });
  });
});
