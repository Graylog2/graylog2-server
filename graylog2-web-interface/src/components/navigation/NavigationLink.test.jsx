import React from 'react';
import { mount, shallow } from 'enzyme';
import NavigationLink from './NavigationLink';

jest.mock('util/URLUtils', () => ({ appPrefixed: path => `/prefix${path}` }));

describe('NavigationLink', () => {
  it('renders with simple props', () => {
    const wrapper = mount(<NavigationLink description="Hello there!" path="/hello" />);
    expect(wrapper.find('LinkContainer')).toHaveProp('to', '/prefix/hello');
    expect(wrapper.find('MenuItem')).toHaveText('Hello there!');
  });
  it('passes props to LinkContainer', () => {
    const wrapper = shallow(<NavigationLink description="Hello there!" path="/hello" someProp={42} />);
    expect(wrapper.find('LinkContainer')).toHaveProp('someProp', 42);
  });
});
