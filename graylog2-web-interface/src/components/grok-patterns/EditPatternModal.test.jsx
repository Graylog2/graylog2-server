import React from 'react';
import { mount } from 'theme/enzymeWithTheme';
import 'helpers/mocking/react-dom_mock';

import EditPatternModal from 'components/grok-patterns/EditPatternModal';

describe('<EditPatternModal />', () => {
  it('should render a modal button with as edit', () => {
    const wrapper = mount(<EditPatternModal savePattern={() => {}}
                                            testPattern={() => {}}
                                            validPatternName={() => {}} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render a modal button with as create', () => {
    const wrapper = mount(<EditPatternModal create
                                            savePattern={() => {}}
                                            testPattern={() => {}}
                                            validPatternName={() => {}} />);
    expect(wrapper).toMatchSnapshot();
  });
});
