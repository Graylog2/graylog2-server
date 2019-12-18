// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';

// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';

import SearchMetadata from 'views/logic/search/SearchMetadata';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import WithSearchStatus from './WithSearchStatus';
import SearchBar from './SearchBar';

const SearchBarWithStatus = WithSearchStatus(SearchBar);

jest.mock('stores/connect', () => (Component, _, propsMapper) => props => <Component {...(Object.assign({}, props, propsMapper(props)))} />);
jest.mock('views/stores/SearchConfigStore', () => ({}));
jest.mock('views/components/SearchBar', () => mockComponent('SearchBar'));

describe('SearchBarWithStatus', () => {
  const configurations = { searchesClusterConfig: {} };
  it('enables search button per default', () => {
    const wrapper = mount(<SearchBarWithStatus configurations={configurations} searchMetadata={SearchMetadata.empty()} executionState={SearchExecutionState.empty()} />);
    expect(wrapper.find('SearchBar')).toHaveProp('disableSearch', false);
  });
});
