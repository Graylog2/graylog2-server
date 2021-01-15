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
import React from 'react';
import { mount, shallow } from 'wrappedEnzyme';

import { appPrefixed } from 'util/URLUtils';

import NavigationLink from './NavigationLink';

jest.mock('util/URLUtils', () => ({ appPrefixed: jest.fn((path) => path), qualifyUrl: jest.fn((path) => path) }));

describe('NavigationLink', () => {
  it('renders with simple props', () => {
    const wrapper = mount(<NavigationLink description="Hello there!" path="/hello" />);

    expect(wrapper.find('LinkContainer')).toHaveProp('to', '/hello');
    expect(wrapper.find('MenuItem')).toHaveText('Hello there!');
  });

  it('passes props to LinkContainer', () => {
    const wrapper = shallow(<NavigationLink description="Hello there!" path="/hello" someProp={42} />);

    expect(wrapper.first()).toHaveProp('someProp', 42);
  });

  it('does not prefix URL with app prefix', () => {
    appPrefixed.mockImplementation((path) => `/someprefix${path}`);
    const wrapper = mount(<NavigationLink description="Hello there!" path="/hello" />);

    expect(wrapper.find('a').props().href).not.toContain('/someprefix/hello');
    expect(appPrefixed).not.toHaveBeenCalled();
  });

  it('renders with NavItem if toplevel', () => {
    const wrapper = mount(<NavigationLink description="Hello there!" path="/hello" topLevel />);

    expect(wrapper.find('LinkContainer')).toHaveProp('to', '/hello');
    expect(wrapper.find('NavItem')).toHaveText('Hello there!');
  });
});
