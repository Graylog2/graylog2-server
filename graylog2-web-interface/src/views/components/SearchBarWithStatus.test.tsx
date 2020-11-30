/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';
import mockComponent from 'helpers/mocking/MockComponent';

import SearchMetadata from 'views/logic/search/SearchMetadata';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

import WithSearchStatus from './WithSearchStatus';
import SearchBar from './SearchBar';

const SearchBarWithStatus = WithSearchStatus(SearchBar);

jest.mock('stores/connect', () => (Component, _, propsMapper) => (props) => <Component {...({ ...props, ...propsMapper(props) })} />);
jest.mock('views/stores/SearchConfigStore', () => ({}));
jest.mock('views/components/SearchBar', () => mockComponent('SearchBar'));

describe('SearchBarWithStatus', () => {
  const configurations = { searchesClusterConfig: {} };

  it('enables search button per default', () => {
    // @ts-ignore
    const wrapper = mount(<SearchBarWithStatus configurations={configurations} searchMetadata={SearchMetadata.empty()} executionState={SearchExecutionState.empty()} />);

    expect(wrapper.find('SearchBar')).toHaveProp('disableSearch', false);
  });
});
