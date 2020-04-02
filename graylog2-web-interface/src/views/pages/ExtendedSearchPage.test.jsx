// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import { act } from 'react-dom/test-utils';

import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';
import { StreamsActions } from 'views/stores/StreamsStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import { QueryFiltersStore } from 'views/stores/QueryFiltersStore';
import { SearchActions, SearchStore } from 'views/stores/SearchStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchConfigActions } from 'views/stores/SearchConfigStore';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import { FieldTypesActions } from 'views/stores/FieldTypesStore';
import { SearchMetadataActions, SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import View from 'views/logic/views/View';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import ExtendedSearchPage from './ExtendedSearchPage';

jest.mock('react-router', () => ({ withRouter: (x) => x }));
jest.mock('components/layout/Footer', () => <div />);
jest.mock('util/History', () => ({ push: jest.fn() }));
jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({
      activeQuery: 'beef-dead',
    })],
  ),
}));
jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    execute: jest.fn(() => Promise.resolve()),
  },
  SearchStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', jest.fn(() => ({
      result: {
        forId: jest.fn(() => {
          return {};
        }),
      },
    }))],
  ),
}));
jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesActions: {},
  FieldTypesStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({
      all: {},
      queryFields: {
        get: jest.fn(() => {
          return {};
        }),
      },
    })],
  ),
}));
jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({
      searchesClusterConfig: {},
    }),
  },
  SearchConfigActions: {},
}));
jest.mock('components/layout/Footer', () => mockComponent('Footer'));
jest.mock('views/components/QueryBar', () => mockComponent('QueryBar'));
jest.mock('views/components/SearchResult', () => mockComponent('SearchResult'));
jest.mock('views/stores/StreamsStore', () => ({ StreamsActions: { refresh: jest.fn() } }));
jest.mock('views/components/common/WindowLeaveMessage', () => mockComponent('WindowLeaveMessage'));
jest.mock('views/components/WithSearchStatus', () => (x) => x);
jest.mock('views/components/SearchBar', () => mockComponent('SearchBar'));
jest.mock('views/components/DashboardSearchBar', () => mockComponent('DashboardSearchBar'));
jest.mock('views/stores/SearchMetadataStore', () => ({ SearchMetadataActions: {}, SearchMetadataStore: {} }));
jest.mock('views/logic/withPluginEntities', () => (x) => x);
jest.mock('views/components/views/CurrentViewTypeProvider', () => jest.fn());

const mockPromise = (res) => {
  const promise = Promise.resolve(res);
  // $FlowFixMe: On purpose for a promise that does not need to be resolved
  promise.then = (x) => x(res);
  return promise;
};

describe('ExtendedSearchPage', () => {
  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    // $FlowFixMe: Exact promise type not required for test functionality
    SearchActions.execute = jest.fn(() => mockPromise());
    StreamsActions.refresh = jest.fn();
    SearchConfigActions.refresh = jest.fn();
    SearchExecutionStateStore.listen = jest.fn(() => jest.fn());
    ViewActions.search.completed.listen = jest.fn(() => jest.fn());
    ViewStore.getInitialState = jest.fn(() => ({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
      dirty: false,
      isNew: true,
      activeQuery: 'foobar',
    }));
    FieldTypesActions.all = mockAction(jest.fn());
    SearchMetadataActions.parseSearch = mockAction(jest.fn(() => mockPromise(SearchMetadata.empty())));
    SearchMetadataStore.listen = jest.fn(() => jest.fn());
    // $FlowFixMe: Somehow flow does not see the `listen` property.
    SearchActions.refresh = mockAction(jest.fn(() => Promise.resolve()));
    asMock(CurrentViewTypeProvider).mockImplementation(({ children }) => <ViewTypeContext.Provider value={View.Type.Dashboard}>{children}</ViewTypeContext.Provider>);
  });

  const SimpleExtendedSearchPage = (props) => (
    <ExtendedSearchPage route={{}}
                        location={{ query: {} }}
                        searchRefreshHooks={[]}
                        {...props} />
  );

  it('register a WindowLeaveMessage', () => {
    const wrapper = mount(<SimpleExtendedSearchPage />);

    expect(wrapper.find('WindowLeaveMessage')).toHaveLength(1);
  });
  it('passes the given route to the WindowLeaveMessage component', () => {
    const route = { path: '/foo' };
    const wrapper = mount(<SimpleExtendedSearchPage route={route} />);

    const windowLeaveMessage = wrapper.find('WindowLeaveMessage');
    expect(windowLeaveMessage).toHaveLength(1);
    expect(windowLeaveMessage).toHaveProp('route', route);
  });

  it('executes search upon mount', () => {
    mount(<SimpleExtendedSearchPage />);

    expect(SearchActions.execute).toHaveBeenCalled();
  });

  it('refreshes search config upon mount', () => {
    mount(<SimpleExtendedSearchPage />);

    expect(SearchConfigActions.refresh).toHaveBeenCalled();
  });

  it('does not register to WidgetStore upon mount', () => {
    mount(<SimpleExtendedSearchPage />);

    expect(WidgetStore.listen).not.toHaveBeenCalled();
  });
  it('does not unregister from Widget store upon unmount', () => {
    const unsubscribe = jest.fn();
    WidgetStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<SimpleExtendedSearchPage />);

    wrapper.unmount();
    expect(unsubscribe).not.toHaveBeenCalled();
  });
  it('does not register to QueryFiltersStore upon mount', () => {
    mount(<SimpleExtendedSearchPage />);

    expect(QueryFiltersStore.listen).not.toHaveBeenCalled();
  });
  it('does not unregister from Query Filter store upon unmount', () => {
    const unsubscribe = jest.fn();
    QueryFiltersStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<SimpleExtendedSearchPage />);

    wrapper.unmount();
    expect(unsubscribe).not.toHaveBeenCalled();
  });
  it('registers to SearchActions.refresh upon mount', () => {
    mount(<SimpleExtendedSearchPage />);

    expect(SearchActions.refresh.listen).toHaveBeenCalled();
  });
  it('registers to ViewActions.search.completed upon mount', () => {
    mount(<SimpleExtendedSearchPage />);

    expect(ViewActions.search.completed.listen).toHaveBeenCalled();
  });
  it('registers to ViewActions.search.completed even if search refresh condition fails', () => {
    mount(<SimpleExtendedSearchPage searchRefreshHools={[() => false]} />);

    expect(ViewActions.search.completed.listen).toHaveBeenCalled();
  });
  it('unregisters from ViewActions.search.completed upon unmount', () => {
    const unsubscribe = jest.fn();
    ViewActions.search.completed.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<SimpleExtendedSearchPage />);

    expect(unsubscribe).not.toHaveBeenCalled();
    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
  it('refreshes Streams upon mount', () => {
    mount(<SimpleExtendedSearchPage />);

    expect(StreamsActions.refresh).toHaveBeenCalled();
  });
  it('updating search in view triggers search execution', () => {
    mount(<SimpleExtendedSearchPage />);

    const cb = asMock(ViewActions.search.completed.listen).mock.calls[0][0];
    asMock(SearchActions.execute).mockClear();
    expect(SearchActions.execute).not.toHaveBeenCalled();

    return cb({ search: {} })
      .then(() => {
        expect(SearchActions.execute).toHaveBeenCalled();
      });
  });
  it('refreshes field types store upon mount', () => {
    expect(FieldTypesActions.all).not.toHaveBeenCalled();
    mount(<SimpleExtendedSearchPage />);
    expect(FieldTypesActions.all).toHaveBeenCalled();
  });
  it('refreshes field types upon every search execution', () => {
    mount(<SimpleExtendedSearchPage />);

    asMock(FieldTypesActions.all).mockClear();
    const cb = asMock(ViewActions.search.completed.listen).mock.calls[0][0];
    return cb({ search: {} })
      .then(() => {
        expect(FieldTypesActions.all).toHaveBeenCalled();
      });
  });

  it('refreshing after query change parses search metadata first', (done) => {
    const wrapper = mount(<SimpleExtendedSearchPage />);

    const searchBar = wrapper.find('DashboardSearchBar');
    const cb = searchBar.at(0).props().onExecute;

    const view = { search: {} };

    const promise = cb(view);

    promise.then(() => {
      expect(SearchMetadataActions.parseSearch).toHaveBeenCalled();
      expect(SearchActions.execute).toHaveBeenCalled();
      done();
    });
  });

  it('changing current query in view does not trigger search execution', () => {
    mount(<SimpleExtendedSearchPage />);

    asMock(SearchActions.execute).mockClear();
    expect(SearchActions.execute).not.toHaveBeenCalled();

    return ViewActions.selectQuery('someQuery')
      .then(() => {
        expect(SearchActions.execute).not.toHaveBeenCalled();
      });
  });

  it('displays SearhStore errors', () => {
    asMock(SearchStore.getInitialState).mockReturnValueOnce({ errors: [new Error('The error message')] });
    const wrapper = mount(<SimpleExtendedSearchPage />);

    expect(wrapper.contains('The error message.')).not.toBeNull();
  });
});
