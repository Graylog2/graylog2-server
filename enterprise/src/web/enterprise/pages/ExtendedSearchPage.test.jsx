// @flow
import React from 'react';
import Immutable from 'immutable';
import { mount, shallow } from 'enzyme';

// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';
import { StreamsActions } from 'enterprise/stores/StreamsStore';
import { WidgetStore } from 'enterprise/stores/WidgetStore';
import { QueryFiltersStore } from 'enterprise/stores/QueryFiltersStore';
import SearchActions from 'enterprise/actions/SearchActions';
import { SearchExecutionStateActions } from 'enterprise/stores/SearchExecutionStateStore';
import { SearchParameterActions } from 'enterprise/stores/SearchParameterStore';
import Parameter from 'enterprise/logic/parameters/Parameter';
import ParameterBinding from 'enterprise/logic/parameters/ParameterBinding';
import SearchExecutionState from 'enterprise/logic/search/SearchExecutionState';

import ExtendedSearchPage from './ExtendedSearchPage';

jest.mock('enterprise/components/QueryBar', () => mockComponent('QueryBar'));
jest.mock('enterprise/components/SearchResult', () => mockComponent('SearchResult'));
jest.mock('enterprise/stores/StreamsStore', () => ({ StreamsActions: { refresh: jest.fn() } }));
jest.mock('enterprise/components/common/WindowLeaveMessage', () => mockComponent('WindowLeaveMessage'));
jest.mock('stores/connect', () => x => x);
jest.mock('enterprise/components/parameters/ParametersWithParameterBindings', () => () => null);
jest.mock('enterprise/components/SearchBarWithStatus', () => mockComponent('SearchBar'));
jest.mock('enterprise/stores/SearchConfigStore', () => ({}));
jest.mock('enterprise/components/parameters/ParameterBarWithUndeclaredParameters', () => mockComponent('ParameterBarWithUndeclaredParameters'));

describe('ExtendedSearchPage', () => {
  beforeEach(() => {
    WidgetStore.listen = jest.fn(() => jest.fn());
    QueryFiltersStore.listen = jest.fn(() => jest.fn());
    // $FlowFixMe: Exact promise type not required for test functionality
    SearchActions.execute = jest.fn(() => ({ then: fn => fn() }));
    StreamsActions.refresh = jest.fn();
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
  it('does not execute search upon mount if parameters are missing values', () => {
    const parameters = Immutable.fromJS({
      foo: Parameter.create('foo', 'FooTitle', '', 'string', undefined, false, ParameterBinding.empty()),
      bar: Parameter.create('bar', 'BarTitle', '', 'string', undefined, false, ParameterBinding.empty()),
    });

    mount(<ExtendedSearchPage route={{}} parameters={parameters} />);

    expect(SearchActions.execute).not.toHaveBeenCalled();
  });
  it('registers to WidgetStore upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(WidgetStore.listen).toHaveBeenCalled();
  });
  it('registers to QueryFiltersStore upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(QueryFiltersStore.listen).toHaveBeenCalled();
  });
  it('refreshes Streams upon mount', () => {
    mount(<ExtendedSearchPage route={{}} />);

    expect(StreamsActions.refresh).toHaveBeenCalled();
  });

  it('unregister from Widget store upon unmount', () => {
    const unsubscribe = jest.fn();
    WidgetStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    expect(unsubscribe).not.toHaveBeenCalled();
    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
  it('unregister from Query Filter store upon unmount', () => {
    const unsubscribe = jest.fn();
    QueryFiltersStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ExtendedSearchPage route={{}} />);

    expect(unsubscribe).not.toHaveBeenCalled();
    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
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
});
