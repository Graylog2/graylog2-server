// @flow strict
import React from 'react';
import { mount } from 'enzyme';

// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';

import SearchMetadata from 'enterprise/logic/search/SearchMetadata';
import SearchExecutionState from 'enterprise/logic/search/SearchExecutionState';
import SearchBarWithStatus from './SearchBarWithStatus';

jest.mock('stores/connect', () => (Component, _, propsMapper) => props => <Component {...(Object.assign({}, props, propsMapper(props)))} />);
jest.mock('enterprise/stores/SearchConfigStore', () => ({}));
jest.mock('enterprise/components/SearchBar', () => mockComponent('SearchBar'));

describe('SearchBarWithStatus', () => {
  const configurations = { searchesClusterConfig: {} };
  it('enables search button per default', () => {
    const wrapper = mount(<SearchBarWithStatus configurations={configurations} searchMetadata={SearchMetadata.empty()} executionState={SearchExecutionState.empty()} />);
    expect(wrapper.find('SearchBar')).toHaveProp('disableSearch', false);
  });
});
