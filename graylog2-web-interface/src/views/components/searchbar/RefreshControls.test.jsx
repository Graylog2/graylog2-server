import React from 'react';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import { RefreshActions } from 'views/stores/RefreshStore';
import RefreshControls from './RefreshControls';

jest.useFakeTimers();

jest.mock('stores/connect', () => (Component) => (props) => <Component {...({ ...props })} />);
jest.mock('views/stores/RefreshStore', () => ({
  RefreshActions: {
    enable: jest.fn(),
    disable: jest.fn(),
  },
  RefreshStore: {},
}));


describe('RefreshControls', () => {
  describe('rendering', () => {
    const verifyRendering = ({ enabled, interval }) => {
      const wrapper = mount(<RefreshControls refreshConfig={{ enabled, interval }} />);
      expect(wrapper).toMatchSnapshot();
    };

    it.each`
    enabled      | interval
    ${true}      | ${1000}
    ${true}      | ${2000}
    ${true}      | ${5000}
    ${true}      | ${10000}
    ${true}      | ${30000}
    ${true}      | ${60000}
    ${true}      | ${300000}
    ${false}     | ${300000}
    ${false}     | ${1000}
  `('it renders refresh controlls with enabled: $enabled and interval: $interval', verifyRendering);
  });

  it('should start the interval', () => {
    const wrapper = mount(<RefreshControls refreshConfig={{ enabled: false, interval: 1000 }} />);
    wrapper.find('svg.fa-play').simulate('click');
    expect(RefreshActions.enable).toHaveBeenCalled();
  });

  it('should stop the interval', () => {
    const wrapper = mount(<RefreshControls refreshConfig={{ enabled: true, interval: 1000 }} />);
    wrapper.find('svg.fa-pause').simulate('click');
    expect(RefreshActions.disable).toHaveBeenCalled();
  });
});
