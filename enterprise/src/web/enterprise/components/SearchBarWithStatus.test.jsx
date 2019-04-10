// @flow strict
import React from 'react';
import { mount } from 'enzyme';
import * as Immutable from 'immutable';

// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';

import SearchBarWithStatus from './SearchBarWithStatus';
import SearchMetadata from '../logic/search/SearchMetadata';
import SearchExecutionState from '../logic/search/SearchExecutionState';
import ParameterBinding from '../logic/parameters/ParameterBinding';
import Parameter from '../logic/parameters/Parameter';

jest.mock('stores/connect', () => (Component, _, propsMapper) => props => <Component {...(Object.assign({}, props, propsMapper(props)))} />);
jest.mock('enterprise/stores/SearchConfigStore', () => ({}));
jest.mock('enterprise/stores/SearchMetadataStore', () => ({}));
jest.mock('enterprise/stores/SearchExecutionStateStore', () => ({}));
jest.mock('enterprise/components/SearchBar', () => mockComponent('SearchBar'));

describe('SearchBarWithStatus', () => {
  const configurations = { searchesClusterConfig: {} };
  it('enables search button per default', () => {
    const wrapper = mount(<SearchBarWithStatus configurations={configurations} searchMetadata={SearchMetadata.empty()} executionState={SearchExecutionState.empty()} />);
    expect(wrapper.find('SearchBar')).toHaveProp('disableSearch', false);
  });
  it('disables search button when undeclared parameter is present', () => {
    const searchMetadata = { undeclared: Immutable.fromJS(['foo']), used: Immutable.Set() };
    const wrapper = mount(<SearchBarWithStatus configurations={configurations} searchMetadata={searchMetadata} executionState={SearchExecutionState.empty()} />);
    expect(wrapper.find('SearchBar')).toHaveProp('disableSearch', true);
  });
  it('disables search button when used parameter has no binding', () => {
    const searchMetadata = {
      undeclared: Immutable.Set(),
      used: Immutable.Set([
        Parameter.create('foo', 'Foo', 'Foo Value', 'any', '*', false, ParameterBinding.empty()),
      ]),
    };

    const wrapper = mount(<SearchBarWithStatus configurations={configurations} searchMetadata={searchMetadata} executionState={SearchExecutionState.empty()} />);
    expect(wrapper.find('SearchBar')).toHaveProp('disableSearch', true);
  });
  it('does not disable search button when parameter binding with missing value is present, but parameter is not used', () => {
    const searchExecutionState = SearchExecutionState.create(Immutable.fromJS({
      aParameter: ParameterBinding.empty(),
    }));
    const searchMetadata = {
      undeclared: Immutable.Set(),
      used: Immutable.Set(),
    };

    const wrapper = mount(<SearchBarWithStatus configurations={configurations} searchMetadata={searchMetadata} executionState={searchExecutionState} />);
    expect(wrapper.find('SearchBar')).toHaveProp('disableSearch', false);
  });
});
