// @flow
import * as React from 'react';
import { mount } from 'enzyme';
import * as Immutable from 'immutable';
import { startCase } from 'lodash';

import { ViewManagementActions } from 'enterprise/stores/ViewManagementStore';
import ViewDeserializer from 'enterprise/logic/views/ViewDeserializer';
import ViewLoader from 'enterprise/logic/views/ViewLoader';
import View from 'enterprise/logic/views/View';
import Search from 'enterprise/logic/search/Search';
import type { ViewJson } from 'enterprise/logic/views/View';
import { SearchExecutionStateActions } from 'enterprise/stores/SearchExecutionStateStore';
import ParameterBinding from 'enterprise/logic/parameters/ParameterBinding';
import Parameter from 'enterprise/logic/parameters/Parameter';
import { ViewStore } from 'enterprise/stores/ViewStore';

import ShowViewPage from './ShowViewPage';

jest.mock('stores/connect', () => x => x);
jest.mock('enterprise/stores/ViewManagementStore', () => ({ ViewManagementActions: { get: jest.fn(() => Promise.reject()) } }));
jest.mock('enterprise/logic/views/ViewDeserializer', () => jest.fn(x => Promise.resolve(x)));
jest.mock('enterprise/logic/views/ViewLoader', () => jest.fn(x => Promise.resolve(x)));
jest.mock('enterprise/stores/SearchExecutionStateStore', () => ({ SearchExecutionStateActions: {} }));
jest.mock('enterprise/components/RequiredParametersForViewForm', () => 'required-parameters-for-view-form');
jest.mock('./ExtendedSearchPage', () => 'extended-search-page');

const dummyParameter = (name: string, defaultValue = undefined) => Parameter.create(name, startCase(name), `${startCase(name)} Value`, 'any', defaultValue, false, ParameterBinding.empty());
const viewForParameters = (viewJson: ViewJson, parameters: Array<Parameter>) => {
  const search = Search.create().toBuilder().parameters(parameters).build();
  return View.fromJSON(viewJson).toBuilder().search(search).build();
};

describe('ShowViewPage', () => {
  const viewJson = {
    id: 'foo',
    title: 'Foo',
    summary: 'summary',
    description: 'Foo',
    search_id: 'foosearch',
    properties: {},
    state: {},
    dashboard_state: { widgets: [], positions: [] },
    created_at: new Date(),
  };
  it('renders Spinner while loading', () => {
    const wrapper = mount(<ShowViewPage location={{ query: {} }} parameters={Immutable.Map()} params={{ viewId: 'foo' }} route={{}} />);
    expect(wrapper.find('Spinner')).toExist();
  });
  it('listens to ViewStore upon mount', () => {
    let listenCallback = null;
    ViewStore.listen = jest.fn((fn) => { listenCallback = fn; return jest.fn(); });
    mount(<ShowViewPage location={{ query: {} }} parameters={Immutable.Map()} params={{ viewId: 'foo' }} route={{}} />);
    expect(ViewStore.listen).toHaveBeenCalled();
    expect(listenCallback).not.toBeNull();
  });
  it('unregisters from ViewStore upon unmount', () => {
    const unsubscribe = jest.fn();
    ViewStore.listen = jest.fn(() => unsubscribe);
    const wrapper = mount(<ShowViewPage location={{ query: {} }} parameters={Immutable.Map()} params={{ viewId: 'foo' }} route={{}} />);
    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
  it('loads view with id passed from props', () => {
    ViewManagementActions.get = jest.fn(() => Promise.reject());
    mount(<ShowViewPage location={{ query: {} }} parameters={Immutable.Map()} params={{ viewId: 'foo' }} route={{}} />);
    expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');
  });
  it('passes loaded view to ViewDeserializer and ViewLoader', (done) => {
    ViewManagementActions.get = jest.fn(() => Promise.resolve(viewJson));
    SearchExecutionStateActions.setParameterValues = jest.fn();
    const search = Search.create().toBuilder().parameters([]).build();
    // $FlowFixMe: Calling mockImplementation on jest.fn()
    ViewDeserializer.mockImplementation((response: ViewJson) => {
      const view = View.fromJSON(response).toBuilder().search(search).build();
      return Promise.resolve(view);
    });
    mount(<ShowViewPage location={{ query: {} }} parameters={Immutable.Map()} params={{ viewId: 'foo' }} route={{}} />);

    setImmediate(() => {
      expect(ViewDeserializer).toHaveBeenCalledWith(viewJson);
      expect(ViewLoader).toHaveBeenCalled();
      expect(SearchExecutionStateActions.setParameterValues).toHaveBeenCalledWith(Immutable.Map());
      done();
    });
  });
  describe('required parameter handling', () => {
    let listenCallback = null;
    beforeEach(() => {
      ViewStore.listen = jest.fn((fn) => { listenCallback = fn; return jest.fn(); });
      ViewManagementActions.get = jest.fn(() => Promise.resolve(viewJson));
      SearchExecutionStateActions.setParameterValues = jest.fn();
    });
    it('extracts parameter values from query if they exist in search', (done) => {
      const parameters = [dummyParameter('foo'), dummyParameter('bar')];
      const parameterMap = Immutable.Map(parameters.map(p => [p.name, p]));
      const search = Search.create().toBuilder().parameters(parameters).build();
      // $FlowFixMe: Calling mockImplementation on jest.fn()
      ViewDeserializer.mockImplementation((response: ViewJson) => {
        const view = View.fromJSON(response).toBuilder().search(search).build();
        return Promise.resolve(view);
      });

      mount(<ShowViewPage location={{ query: { foo: 42, baz: 23 } }} parameters={parameterMap} params={{ viewId: 'foo' }} route={{}} />);

      setImmediate(() => {
        expect(SearchExecutionStateActions.setParameterValues).toHaveBeenCalledWith(Immutable.fromJS({ foo: 42 }));
        done();
      });
    });
    it('shows parameter enquiry form if parameters are missing', (done) => {
      const parameters = [dummyParameter('foo')];
      const parameterMap = Immutable.Map(parameters.map(p => [p.name, p]));
      const view = viewForParameters(viewJson, parameters);

      // $FlowFixMe: Calling mockImplementation on jest.fn()
      ViewDeserializer.mockImplementation(() => Promise.resolve(view));

      const wrapper = mount(<ShowViewPage location={{ query: {} }}
                                          parameters={parameterMap}
                                          params={{ viewId: 'foo' }}
                                          route={{}} />);

      expect(listenCallback).not.toBeNull();
      if (listenCallback) {
        listenCallback({ view, activeQuery: 'foo', dirty: false });
      }

      setImmediate(() => {
        wrapper.update();
        const requiredParametersForViewForm = wrapper.find('required-parameters-for-view-form');
        expect(requiredParametersForViewForm).toExist();
        expect(requiredParametersForViewForm).toHaveProp('parameters', parameterMap);
        expect(wrapper.find('ExtendedSearchPage')).not.toExist();
        done();
      });
    });
    it('renders required parameter form if parameter values are passed but not all required parameters are fulfilled', (done) => {
      const parameters = [dummyParameter('foo'), dummyParameter('bar')];
      const parameterMap = Immutable.Map(parameters.map(p => [p.name, p]));
      const view = viewForParameters(viewJson, parameters);

      // $FlowFixMe: Calling mockImplementation on jest.fn()
      ViewDeserializer.mockImplementation(() => Promise.resolve(view));

      const wrapper = mount(<ShowViewPage location={{ query: { foo: 23 } }} parameters={parameterMap} params={{ viewId: 'foo' }} route={{}} />);

      expect(listenCallback).not.toBeNull();
      if (listenCallback) {
        listenCallback({ view, activeQuery: 'foo', dirty: false });
      }

      setImmediate(() => {
        wrapper.update();
        const requiredParametersForViewForm = wrapper.find('required-parameters-for-view-form');
        expect(requiredParametersForViewForm).toExist();
        expect(requiredParametersForViewForm).toHaveProp('parameters', Immutable.fromJS({
          foo: dummyParameter('foo').toBuilder().binding(ParameterBinding.forValue(23)).build(),
          bar: dummyParameter('bar'),
        }));
        expect(wrapper.find('ExtendedSearchPage')).not.toExist();
        done();
      });
    });
    it('does not render required parameter form if parameter values are passed and all required parameters are fulfilled', (done) => {
      const parameters = [dummyParameter('foo'), dummyParameter('bar')];
      const parameterMap = Immutable.Map(parameters.map(p => [p.name, p]));
      const view = viewForParameters(viewJson, parameters);

      // $FlowFixMe: Calling mockImplementation on jest.fn()
      ViewDeserializer.mockImplementation(() => Promise.resolve(view));

      const wrapper = mount(<ShowViewPage location={{ query: { foo: 23, bar: 42 } }} parameters={parameterMap} params={{ viewId: 'foo' }} route={{}} />);

      expect(listenCallback).not.toBeNull();
      if (listenCallback) {
        listenCallback({ view, activeQuery: 'foo', dirty: false });
      }

      setImmediate(() => {
        wrapper.update();
        expect(wrapper.find('required-parameters-for-view-form')).not.toExist();
        expect(wrapper.find('extended-search-page')).toExist();
        done();
      });
    });
  });
});
