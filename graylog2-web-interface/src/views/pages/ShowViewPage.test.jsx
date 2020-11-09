// @flow strict
import * as React from 'react';
import { List } from 'immutable';
import { mount } from 'wrappedEnzyme';
import mockAction from 'helpers/mocking/MockAction';
import asMock from 'helpers/mocking/AsMock';

import StreamsContext from 'contexts/StreamsContext';
import { NotFoundErrorType } from 'logic/errors/ReportedErrors';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import type { ViewJson } from 'views/logic/views/View';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { SearchExecutionStateActions } from 'views/stores/SearchExecutionStateStore';

import ShowViewPage from './ShowViewPage';

jest.mock('stores/connect', () => (x) => x);
jest.mock('views/logic/views/ViewDeserializer', () => jest.fn((x) => Promise.resolve(x)));

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

jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    execute: mockAction(jest.fn()),
  },
}));

jest.mock('views/stores/SearchExecutionStateStore', () => ({
  SearchExecutionStateActions: {},
  SearchExecutionStateStore: { listen: jest.fn() },
}));

jest.mock('actions/errors/ErrorsActions', () => ({
  report: jest.fn(),
}));

jest.mock('views/components/Search', () => 'extended-search-page');
jest.mock('routing/withLocation', () => (x) => x);
jest.mock('routing/withParams', () => (x) => x);

describe('ShowViewPage', () => {
  const viewJson: ViewJson = {
    id: 'foo',
    type: 'DASHBOARD',
    title: 'Foo',
    summary: 'summary',
    description: 'Foo',
    search_id: 'foosearch',
    properties: List<any>(),
    state: {},
    dashboard_state: { widgets: [], positions: [] },
    created_at: new Date(),
    owner: 'admin',
    requires: {},
  };
  const SimpleShowViewPage = (props) => (
    <StreamsContext.Provider value={[{ id: 'stream-id-1' }]}>
      <ShowViewPage location={{ query: {} }}
                    params={{ viewId: 'foo' }}
                    {...props} />
    </StreamsContext.Provider>
  );

  beforeEach(() => {
    jest.resetAllMocks();
    jest.resetModules();
  });

  it('renders Spinner while loading', () => {
    const mockGet = () => new Promise<ViewJson>((resolve) => setTimeout(resolve, 30000, viewJson));

    asMock(ViewManagementActions.get).mockImplementation(mockAction(mockGet));
    const wrapper = mount(<SimpleShowViewPage />);

    expect(wrapper.find('Spinner')).toExist();
  });

  it('loads view with id passed from props', () => {
    asMock(ViewManagementActions.get).mockImplementation(mockAction(jest.fn(() => Promise.reject())));
    mount(<SimpleShowViewPage />);

    expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');
  });

  it('reports NotForundError error if loading view returns 404', (done) => {
    const error = new Error('Not found');

    // $FlowFixMe: Assigning to non-existing key on purpose
    error.status = 404;
    asMock(ViewManagementActions.get).mockImplementation(mockAction(jest.fn(() => Promise.reject(error))));

    mount(<SimpleShowViewPage />);

    setImmediate(() => {
      expect(ErrorsActions.report).toHaveBeenCalledTimes(1);

      expect(ErrorsActions.report).toHaveBeenCalledWith({
        error,
        type: NotFoundErrorType,
      });

      done();
    });
  });

  it('passes loaded view to ViewDeserializer', (done) => {
    asMock(ViewManagementActions.get).mockImplementation(mockAction(jest.fn(() => Promise.resolve(viewJson))));
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

  it('calls ViewLoader again if view id prop changes', () => {
    const viewLoader = jest.fn(() => Promise.resolve());
    const wrapper = mount(<SimpleShowViewPage viewLoader={viewLoader} />);

    expect(viewLoader).toHaveBeenCalledWith('foo', [], [], {}, expect.anything(), expect.anything());

    wrapper.setProps({ params: { viewId: 'bar' } });

    expect(viewLoader).toHaveBeenCalledWith('bar', [], [], {}, expect.anything(), expect.anything());
  });

  it('does not call ViewLoader again if view id prop does not change', () => {
    const viewLoader = jest.fn(() => Promise.resolve());
    const wrapper = mount(<SimpleShowViewPage viewLoader={viewLoader} />);

    expect(viewLoader).toHaveBeenCalledWith('foo', [], [], {}, expect.anything(), expect.anything());

    viewLoader.mockClear();

    wrapper.setProps({ params: { viewId: 'foo' } });

    expect(viewLoader).not.toHaveBeenCalled();
  });
});
