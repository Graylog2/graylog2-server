import React from 'react';
import { mount, shallow } from 'wrappedEnzyme';
import NavigationLink from './NavigationLink';
import URLUtils from '../../util/URLUtils';

jest.mock('util/URLUtils', () => ({ appPrefixed: jest.fn(path => path) }));

describe('NavigationLink', () => {
  it('renders with simple props', () => {
    const wrapper = mount(<NavigationLink description="Hello there!" path="/hello" />);
    expect(wrapper.find('LinkContainer')).toHaveProp('to', '/hello');
    expect(wrapper.find('MenuItem')).toHaveText('Hello there!');
  });
  it('passes props to LinkContainer', () => {
    const wrapper = shallow(<NavigationLink description="Hello there!" path="/hello" someProp={42} />);
    expect(wrapper.find('LinkContainer')).toHaveProp('someProp', 42);
  });
  it('does not prefix URL with app prefix', () => {
    URLUtils.appPrefixed.mockImplementation(path => `/someprefix${path}`);
    const wrapper = shallow(<NavigationLink description="Hello there!" path="/hello" someProp={42} />);
    const linkContainer = wrapper.find('LinkContainer');
    expect(linkContainer.props().to).not.toContain('/someprefix');
    expect(URLUtils.appPrefixed).not.toHaveBeenCalled();
  });
});
