// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import Routes from 'routing/Routes';
import history from 'util/History';

import mockAction from 'helpers/mocking/MockAction';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import type { ViewJson } from 'views/logic/views/View';
import { SearchExecutionStateActions } from 'views/stores/SearchExecutionStateStore';

import ShowViewPage from './ShowViewPage';

jest.mock('stores/connect', () => x => x);
jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    get: jest.fn(() => Promise.reject()),
    update: {
      completed: {
        listen: jest.fn(),
      },
    },
  },
}));
jest.mock('views/logic/views/ViewDeserializer', () => jest.fn(x => Promise.resolve(x)));
jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    execute: mockAction(jest.fn()),
  },
}));
jest.mock('views/stores/SearchExecutionStateStore', () => ({
  SearchExecutionStateActions: {},
  SearchExecutionStateStore: { listen: jest.fn() },
}));
jest.mock('util/History', () => ({}));
jest.mock('./ExtendedSearchPage', () => 'extended-search-page');

describe('ShowViewPage', () => {
  const viewJson = {
    id: 'foo',
    type: 'DASHBOARD',
    title: 'Foo',
    summary: 'summary',
    description: 'Foo',
    search_id: 'foosearch',
    properties: {},
    state: {},
    dashboard_state: { widgets: [], positions: [] },
    created_at: new Date(),
    owner: 'admin',
    requires: {},
  };
  const SimpleShowViewPage = props => (
    <ShowViewPage location={{ query: {} }}
                  params={{ viewId: 'foo' }}
                  route={{}}
                  {...props} />
  );
  beforeEach(() => {
    jest.resetAllMocks();
    jest.resetModules();
  });
  it('renders Spinner while loading', () => {
    const promise = () => new Promise<ViewJson>(resolve => setTimeout(resolve, 30000, viewJson));
    ViewManagementActions.get = mockAction(jest.fn(promise));
    const wrapper = mount(<SimpleShowViewPage />);
    expect(wrapper.find('Spinner')).toExist();
  });
  it('loads view with id passed from props', () => {
    ViewManagementActions.get = mockAction(jest.fn(() => Promise.reject()));
    mount(<SimpleShowViewPage />);
    expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');
  });
  it('redirects to "Not Found" page if loading view returns 404', (done) => {
    const error = new Error('Not found');
    // $FlowFixMe: Assigning to non-existing key on purpose
    error.status = 404;
    ViewManagementActions.get = mockAction(jest.fn(() => Promise.reject(error)));
    history.replace = jest.fn();

    mount(<SimpleShowViewPage />);

    setImmediate(() => {
      expect(history.replace).toHaveBeenCalledWith(Routes.NOTFOUND);
      done();
    });
  });
  it('passes loaded view to ViewDeserializer', (done) => {
    ViewManagementActions.get = mockAction(jest.fn(() => Promise.resolve(viewJson)));
    SearchExecutionStateActions.setParameterValues = mockAction(jest.fn());
    const search = Search.create().toBuilder().parameters([]).build();
    // $FlowFixMe: Calling mockImplementation on jest.fn()
    ViewDeserializer.mockImplementation((response: ViewJson) => {
      const view = View.fromJSON(response).toBuilder().search(search).build();
      return Promise.resolve(view);
    });
    mount(<SimpleShowViewPage />);

    setImmediate(() => {
      expect(ViewDeserializer).toHaveBeenCalledWith(viewJson);
      done();
    });
  });
  it('calls ViewLoader upon mount', () => {
    const viewLoader = jest.fn(() => Promise.resolve());
    mount(<SimpleShowViewPage viewLoader={viewLoader} />);

    expect(viewLoader).toHaveBeenCalled();
  });
});
