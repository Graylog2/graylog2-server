import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import EditPatternModal from 'components/grok-patterns/EditPatternModal';

describe('<EditPatternModal />', () => {

  it('should render a modal button with as edit', () => {
    const wrapper = renderer.create(<EditPatternModal />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

    it('should render a modal button with as create', () => {
    const wrapper = renderer.create(<EditPatternModal create />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
