import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import Wizard from 'components/common/Wizard';

describe('<Wizard />', () => {
  const steps = [
    { key: 'Key1', title: 'Title1', component: (<div>Component1</div>) },
    { key: 'Key2', title: 'Title2', component: (<div>Component2</div>) },
    { key: 'Key3', title: 'Title3', component: (<div>Component3</div>) },
  ];

  it('should render with 3 steps', () => {
    const wrapper = renderer.create(<Wizard steps={steps} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with 3 steps and children', () => {
    const wrapper = renderer.create(<Wizard steps={steps}><span>Preview</span></Wizard>);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

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

  it('should give callback step when changing the step', () => {
    const changeFn = jest.fn((step) => {
      expect(step).toEqual('Key2');
    });

    const wrapper = mount(<Wizard steps={steps} onStepChange={changeFn} />);
    wrapper.find('button[children="Next"]').simulate('click');
    expect(changeFn.mock.calls.length).toBe(1);
  });
});
