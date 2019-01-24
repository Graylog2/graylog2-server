// @flow strict
import React from 'react';
import Immutable from 'immutable';
import { mount, shallow } from 'enzyme';

// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';
import { StreamsActions } from 'enterprise/stores/StreamsStore';
import { WidgetStore } from 'enterprise/stores/WidgetStore';
import { QueryFiltersStore } from 'enterprise/stores/QueryFiltersStore';
import SearchActions from 'enterprise/actions/SearchActions';
import { SearchExecutionStateActions, SearchExecutionStateStore } from 'enterprise/stores/SearchExecutionStateStore';
import { SearchParameterActions, SearchParameterStore } from 'enterprise/stores/SearchParameterStore';
import Parameter from 'enterprise/logic/parameters/Parameter';
import ParameterBinding from 'enterprise/logic/parameters/ParameterBinding';
import SearchExecutionState from 'enterprise/logic/search/SearchExecutionState';
import { SearchConfigActions } from 'enterprise/stores/SearchConfigStore';
import { ViewActions } from 'enterprise/stores/ViewStore';
import { FieldTypesActions } from 'enterprise/stores/FieldTypesStore';
import { SearchMetadataActions, SearchMetadataStore } from 'enterprise/stores/SearchMetadataStore';

import ExtendedSearchPage from './ExtendedSearchPage';

jest.mock('enterprise/components/QueryBar', () => mockComponent('QueryBar'));
jest.mock('enterprise/components/SearchResult', () => mockComponent('SearchResult'));
jest.mock('enterprise/stores/StreamsStore', () => ({ StreamsActions: { refresh: jest.fn() } }));
jest.mock('enterprise/components/common/WindowLeaveMessage', () => mockComponent('WindowLeaveMessage'));
jest.mock('stores/connect', () => x => x);
jest.mock('enterprise/components/parameters/ParametersWithParameterBindings', () => () => null);
jest.mock('enterprise/components/SearchBarWithStatus', () => mockComponent('SearchBar'));
jest.mock('enterprise/stores/SearchConfigStore', () => ({ SearchConfigStore: {}, SearchConfigActions: {} }));
jest.mock('enterprise/components/parameters/ParameterBarWithUndeclaredParameters', () => mockComponent('ParameterBarWithUndeclaredParameters'));
jest.mock('enterprise/stores/FieldTypesStore', () => ({ FieldTypesActions: {} }));
jest.mock('enterprise/stores/SearchMetadataStore', () => ({ SearchMetadataActions: {}, SearchMetadataStore: {} }));

describe('ExtendedSearchPage', () => {
  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    // $FlowFixMe: Exact promise type not required for test functionality
    SearchActions.execute = jest.fn(() => ({ then: fn => fn() }));
    StreamsActions.refresh = jest.fn();
    SearchConfigActions.refresh = jest.fn();
    SearchExecutionStateStore.listen = jest.fn(() => jest.fn());
    SearchParameterStore.listen = jest.fn(() => jest.fn());
    ViewActions.search.completed.listen = jest.fn(() => jest.fn());
    ViewActions.selectQuery.completed.listen = jest.fn(() => jest.fn());
    FieldTypesActions.all = jest.fn();
    SearchMetadataActions.parseSearch = jest.fn();
    SearchMetadataStore.listen = jest.fn(() => jest.fn());
  });

  it('register a WindowLeaveMessage', () => {
    const wrapper = shallow(<ExtendedSearchPage route={{}} />);

    expect(wrapper.find('WindowLeaveMessage')).toHaveLength(1);
  });
  it('passes the given route to the WindowLeaveMessage component', () => {
    const route = { path: '/foo' };
    const wrapper = shallow(<ExtendedSearchPage route={route} />);

    const windowLeaveMessage = wrapper.find('WindowLeaveMessage');
    expect(windowLeaveMessage).toHaveLength(1);
    expect(windowLeaveMessage).toHaveProp('route', route);
  });

  it('executes search upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(SearchActions.execute).toHaveBeenCalled();
  });

  it('refreshes search config upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(SearchConfigActions.refresh).toHaveBeenCalled();
  });

  it('does not execute search upon mount if parameters are missing values', () => {
    const parameters = Immutable.fromJS({
      foo: Parameter.create('foo', 'FooTitle', '', 'string', undefined, false, ParameterBinding.empty()),
      bar: Parameter.create('bar', 'BarTitle', '', 'string', undefined, false, ParameterBinding.empty()),
    });

    mount(<ExtendedSearchPage route={{}} parameters={parameters} />);

    expect(SearchActions.execute).not.toHaveBeenCalled();
  });
  it('does not register to WidgetStore upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(WidgetStore.listen).not.toHaveBeenCalled();
  });
  it('does not unregister from Widget store upon unmount', () => {
    const unsubscribe = jest.fn();
    WidgetStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    wrapper.unmount();
    expect(unsubscribe).not.toHaveBeenCalled();
  });
  it('does not register to QueryFiltersStore upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(QueryFiltersStore.listen).not.toHaveBeenCalled();
  });
  it('does not unregister from Query Filter store upon unmount', () => {
    const unsubscribe = jest.fn();
    QueryFiltersStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    wrapper.unmount();
    expect(unsubscribe).not.toHaveBeenCalled();
  });
  it('registers to SearchExecutionStateStore upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(SearchExecutionStateStore.listen).toHaveBeenCalled();
  });

  it('unregisters from SearchExecutionStateStore upon unmount', () => {
    const unsubscribe = jest.fn();
    SearchExecutionStateStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    expect(unsubscribe).not.toHaveBeenCalled();
    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
  it('registers to SearchParameterStore upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(SearchParameterStore.listen).toHaveBeenCalled();
  });

  it('unregisters from SearchParameterStore upon unmount', () => {
    const unsubscribe = jest.fn();
    SearchParameterStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    expect(unsubscribe).not.toHaveBeenCalled();
    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
  it('registers to ViewActions.search.completed upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(ViewActions.search.completed.listen).toHaveBeenCalled();
  });
  it('unregisters from ViewActions.search.completed upon unmount', () => {
    const unsubscribe = jest.fn();
    ViewActions.search.completed.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    expect(unsubscribe).not.toHaveBeenCalled();
    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
  it('refreshes Streams upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(StreamsActions.refresh).toHaveBeenCalled();
  });
  it('saves newly declared parameter and assigns default value', () => {
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    const parameterBar = wrapper.find('ParameterBarWithUndeclaredParameters');
    const { onParameterSave } = parameterBar.at(0).props();

    const parameters = Immutable.fromJS({
      hostname: Parameter.create('hostname', 'Hostname', '', 'string', 'localhost', false, ParameterBinding.empty()),
      destination: Parameter.create('destination', 'Destination', '', 'string', undefined, false, ParameterBinding.empty()),
    });

    SearchParameterActions.declare = jest.fn((newParameters) => {
      expect(newParameters).toEqual(parameters);
      // $FlowFixMe: Poor man's mocking of Promise, enforcing immediate execution.
      return { then: fn => fn() };
    });
    SearchExecutionStateActions.bindParameterValue = jest.fn((name, defaultValue) => {
      expect(name).toBe('hostname');
      expect(defaultValue).toBe('localhost');
      return Promise.resolve(SearchExecutionState.empty());
    });

    onParameterSave(parameters, ['hostname', 'destination']);

    expect(SearchParameterActions.declare).toHaveBeenCalled();
  });
  it('updating search in view triggers search execution', () => {
    const searchMetadata = { undeclared: Immutable.Set() };
    SearchMetadataActions.parseSearch.mockReturnValue(Promise.resolve(searchMetadata));
    mount(<ExtendedSearchPage route={{}} />);

    const cb = ViewActions.search.completed.listen.mock.calls[0][0];
    SearchActions.execute.mockClear();
    expect(SearchActions.execute).not.toHaveBeenCalled();

    return cb({ search: {} })
      .then(() => {
        expect(SearchActions.execute).toHaveBeenCalled();
      });
  });
  it('updating search parameters triggers search execution', () => {
    mount(<ExtendedSearchPage route={{}} />);

    const cb = SearchParameterStore.listen.mock.calls[0][0];
    SearchActions.execute.mockClear();
    expect(SearchActions.execute).not.toHaveBeenCalled();

    cb();

    expect(SearchActions.execute).toHaveBeenCalled();
  });
  it('updating search execution state does not trigger search execution', () => {
    mount(<ExtendedSearchPage route={{}} />);

    const cb = SearchExecutionStateStore.listen.mock.calls[0][0];
    SearchActions.execute.mockClear();
    expect(SearchActions.execute).not.toHaveBeenCalled();

    cb();

    expect(SearchActions.execute).not.toHaveBeenCalled();
  });
  it('refreshes field types store upon mount', () => {
    expect(FieldTypesActions.all).not.toHaveBeenCalled();
    mount(<ExtendedSearchPage route={{}} />);
    expect(FieldTypesActions.all).toHaveBeenCalled();
  });
  it('refreshes field types upon every search execution', () => {
    const searchMetadata = { undeclared: Immutable.Set() };
    SearchMetadataActions.parseSearch.mockReturnValue(Promise.resolve(searchMetadata));
    mount(<ExtendedSearchPage route={{}} />);

    FieldTypesActions.all.mockClear();
    const cb = ViewActions.search.completed.listen.mock.calls[0][0];
    return cb({ search: {} })
      .then(() => {
        expect(FieldTypesActions.all).toHaveBeenCalled();
      });
  });

  it('refreshing after query change parses search metadata first', (done) => {
    const searchMetadata = { undeclared: Immutable.Set() };
    SearchMetadataActions.parseSearch.mockReturnValue(Promise.resolve(searchMetadata));
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    const searchBar = wrapper.find('SearchBar');
    const cb = searchBar.at(0).props().onExecute;

    const view = { search: {} };

    const promise = cb(view);

    promise.then(() => {
      expect(SearchMetadataActions.parseSearch).toHaveBeenCalled();
      expect(SearchActions.execute).toHaveBeenCalled();
      done();
    });
  });

  it('not refreshing after query change if undeclared parameters are present', (done) => {
    const searchMetadata = { undeclared: Immutable.Set(['foo']) };
    SearchMetadataActions.parseSearch.mockReturnValue(Promise.resolve(searchMetadata));
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    SearchActions.execute.mockClear();
    const searchBar = wrapper.find('SearchBar');
    const cb = searchBar.at(0).props().onExecute;

    const view = { search: {} };

    const promise = cb(view);

    promise.catch(() => {
      expect(SearchMetadataActions.parseSearch).toHaveBeenCalled();
      expect(SearchActions.execute).not.toHaveBeenCalled();
      done();
    });
  });
  it('changing current query in view triggers search execution', () => {
    const searchMetadata = { undeclared: Immutable.Set() };
    SearchMetadataActions.parseSearch.mockReturnValue(Promise.resolve(searchMetadata));
    mount(<ExtendedSearchPage route={{}} />);

    const cb = ViewActions.selectQuery.completed.listen.mock.calls[0][0];
    SearchActions.execute.mockClear();
    expect(SearchActions.execute).not.toHaveBeenCalled();

    return cb({ search: {} })
      .then(() => {
        expect(SearchActions.execute).toHaveBeenCalled();
      });
  });
});
