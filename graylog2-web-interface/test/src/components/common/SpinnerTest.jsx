import React from 'react';
import { shallow } from 'enzyme';
import sinon from 'sinon';

import { Spinner } from 'components/common';

describe('<Spinner />', () => {
  it('should render without props', () => {
    const wrapper = shallow(<Spinner />);
    expect(wrapper).toBeDefined();
    expect(wrapper.find('.fa-spinner').length).toBe(1);
    expect(wrapper.text()).toContain('Loading...');
  });

  it('should render with a different text string', () => {
    const text = 'Hello world!';
    const wrapper = shallow(<Spinner text={text}/>);
    expect(wrapper).toBeDefined();
    expect(wrapper.find('.fa-spinner').length).toBe(1);
    expect(wrapper.text()).toContain(text);
  });

});
