import React from 'react';
import renderer from 'react-test-renderer';

import Spinner from 'components/common/Spinner';

describe('<Spinner />', () => {
  it('should render without props', () => {
    const wrapper = renderer.create(<Spinner />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with a different text string', () => {
    const text = 'Hello world!';
    const wrapper = renderer.create(<Spinner text={text}/>);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

});
