import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import EditPatternModal from 'components/grok-patterns/EditPatternModal';

describe('<EditPatternModal />', () => {
  it('should render a modal button with as edit', () => {
    const wrapper = renderer.create(<EditPatternModal savePattern={() => {}}
                                                      testPattern={() => {}}
                                                      validPatternName={() => {}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a modal button with as create', () => {
    const wrapper = renderer.create(<EditPatternModal create
                                                      savePattern={() => {}}
                                                      testPattern={() => {}}
                                                      validPatternName={() => {}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
