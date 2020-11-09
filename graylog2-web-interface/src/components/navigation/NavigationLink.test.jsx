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
