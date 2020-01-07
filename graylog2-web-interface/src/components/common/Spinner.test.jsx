import React from 'react';
import { mount } from 'wrappedEnzyme';

import Spinner from 'components/common/Spinner';

describe('<Spinner />', () => {
  it('should render without props', () => {
    const wrapper = mount(<Spinner />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with a different text string', () => {
    const text = 'Hello world!';
    const wrapper = mount(<Spinner text={text} />);
    expect(wrapper).toMatchSnapshot();
  });
});
