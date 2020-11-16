/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { mount } from 'wrappedEnzyme';

import Wizard from 'components/common/Wizard';

import 'helpers/mocking/react-dom_mock';

describe('<Wizard />', () => {
  const steps = [
    { key: 'Key1', title: 'Title1', component: (<div>Component1</div>) },
    { key: 'Key2', title: 'Title2', component: (<div>Component2</div>) },
    { key: 'Key3', title: 'Title3', component: (<div>Component3</div>) },
  ];

  it('should render with 3 steps', () => {
    const wrapper = mount(<Wizard steps={steps} />);

    expect(wrapper).toExist();
  });

  it('should render with 3 steps and children', () => {
    const wrapper = mount(<Wizard steps={steps}><span>Preview</span></Wizard>);

    expect(wrapper).toExist();
  });

  it('should render in horizontal mode with 3 steps', () => {
    const wrapper = mount(<Wizard steps={steps} horizontal />);

    expect(wrapper).toExist();
  });

  it('should render in horizontal mode with 3 steps and children', () => {
    const wrapper = mount(<Wizard steps={steps} horizontal><span>Preview</span></Wizard>);

    expect(wrapper).toExist();
  });

  describe('When used in an uncontrolled way', () => {
    it('should render step 1 when nothing was clicked', () => {
      const wrapper = mount(<Wizard steps={steps} />);

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
      expect(wrapper.find('button[children="Previous"]').prop('disabled')).toBe(true);
      expect(wrapper.find('button[children="Next"]').prop('disabled')).toBe(false);
    });

    it('should render step 2 when clicked on step 2', () => {
      const wrapper = mount(<Wizard steps={steps} />);

      wrapper.find('a[children="Title2"]').simulate('click');

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
      expect(wrapper.find('button[children="Previous"]').prop('disabled')).toBe(false);
      expect(wrapper.find('button[children="Next"]').prop('disabled')).toBe(false);
    });

    it('should render step 2 when clicked on next', () => {
      const wrapper = mount(<Wizard steps={steps} />);

      wrapper.find('button[children="Next"]').simulate('click');

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
      expect(wrapper.find('button[children="Previous"]').prop('disabled')).toBe(false);
      expect(wrapper.find('button[children="Next"]').prop('disabled')).toBe(false);
    });

    it('should render step 3 when two times clicked on next', () => {
      const wrapper = mount(<Wizard steps={steps} />);

      wrapper.find('button[children="Next"]').simulate('click');
      wrapper.find('button[children="Next"]').simulate('click');

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(true);
      expect(wrapper.find('button[children="Previous"]').prop('disabled')).toBe(false);
      expect(wrapper.find('button[children="Next"]').prop('disabled')).toBe(true);
    });

    it('should render step 2 when two times clicked on next and one time clicked on previous', () => {
      const changeFn = jest.fn(() => {});
      const wrapper = mount(<Wizard steps={steps} onStepChange={changeFn} />);

      wrapper.find('button[children="Next"]').simulate('click');
      wrapper.find('button[children="Next"]').simulate('click');
      wrapper.find('button[children="Previous"]').simulate('click');

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
      expect(wrapper.find('button[children="Previous"]').prop('disabled')).toBe(false);
      expect(wrapper.find('button[children="Next"]').prop('disabled')).toBe(false);
      expect(changeFn.mock.calls.length).toBe(3);
    });
  });

  describe('When used in a controlled way', () => {
    it('should render active step given from prop', () => {
      const wrapper = mount(<Wizard steps={steps} activeStep="Key2" />);

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);

      wrapper.find('button[children="Next"]').simulate('click');

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
    });

    it('should change the active step when prop changes', () => {
      const wrapper = mount(<Wizard steps={steps} activeStep="Key2" />);

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);

      wrapper.setProps({ activeStep: 'Key1' });

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
    });

    it('should show a warning when activeStep is not a key in steps', () => {
      /* eslint-disable no-console */
      const consoleWarn = console.warn;

      console.warn = jest.fn();
      const wrapper = mount(<Wizard steps={steps} activeStep={0} />);

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
      expect(console.warn.mock.calls.length).toBe(1);

      wrapper.setProps({ activeStep: 'Key12314' });

      expect(wrapper.find('div[children="Component1"]').exists()).toBe(true);
      expect(wrapper.find('div[children="Component2"]').exists()).toBe(false);
      expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
      expect(console.warn.mock.calls.length).toBe(2);

      console.warn = consoleWarn;
      /* eslint-enable no-console */
    });
  });

  it('should give callback step when changing the step', () => {
    const changeFn = jest.fn((step) => {
      expect(step).toEqual('Key2');
    });

    const wrapper = mount(<Wizard steps={steps} onStepChange={changeFn} />);

    wrapper.find('button[children="Next"]').simulate('click');

    expect(changeFn.mock.calls.length).toBe(1);

    const controlledWrapped = mount(<Wizard steps={steps} onStepChange={changeFn} activeStep="Key1" />);

    controlledWrapped.find('button[children="Next"]').simulate('click');

    expect(changeFn.mock.calls.length).toBe(2);
  });

  it('should respect disabled flag for a step', () => {
    steps[1].disabled = true;
    steps[2].disabled = true;
    const wrapper = mount(<Wizard steps={steps} />);

    wrapper.find('button[children="Next"]').simulate('click');

    expect(wrapper.find('div[children="Component1"]').exists()).toBe(true);
    expect(wrapper.find('div[children="Component2"]').exists()).toBe(false);
    expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);

    wrapper.find('a[children="Title2"]').simulate('click');

    expect(wrapper.find('div[children="Component1"]').exists()).toBe(true);
    expect(wrapper.find('div[children="Component2"]').exists()).toBe(false);
    expect(wrapper.find('div[children="Component3"]').exists()).toBe(false);
  });

  it('should render next/previous buttons by default', () => {
    const wrapperV = mount(<Wizard steps={steps} />);

    expect(wrapperV.find('button[children="Next"]').exists()).toBe(true);
    expect(wrapperV.find('button[children="Previous"]').exists()).toBe(true);

    const wrapperH = mount(<Wizard steps={steps} horizontal />);

    expect(wrapperH.find('svg.fa-caret-left').exists()).toBe(true);
    expect(wrapperH.find('svg.fa-caret-right').exists()).toBe(true);
  });

  it('should hide next/previous buttons if hidePreviousNextButtons is set', () => {
    const wrapperV = mount(<Wizard steps={steps} hidePreviousNextButtons />);

    expect(wrapperV.find('button[children="Next"]').exists()).toBe(false);
    expect(wrapperV.find('button[children="Previous"]').exists()).toBe(false);

    const wrapperH = mount(<Wizard steps={steps} horizontal hidePreviousNextButtons />);

    expect(wrapperH.find('button > svg.fa-caret-left').exists()).toBe(false);
    expect(wrapperH.find('button > svg.fa-caret-right').exists()).toBe(false);
  });
});
