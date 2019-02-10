import React from 'react';
import { mount } from 'enzyme';
import TimeUnitInput from './TimeUnitInput';

describe('<TimeUnitInput />', () => {
  it('should have right default values from props', () => {
    const onUpdate = (value, unit, checked) => {
      expect(value).toBe(1);
      expect(unit).toBe('SECONDS');
      expect(checked).toBe(true);
    };

    const wrapper = mount(<TimeUnitInput update={onUpdate} />);
    const checkbox = wrapper.find('input[type="checkbox"]');
    expect(checkbox.prop('checked')).toBe(false);
    expect(wrapper.find('input[type="number"]').prop('value')).toBe(1);
    checkbox.simulate('click');
  });

  it('should use custom default values', () => {
    const onUpdate = (value, unit, checked) => {
      expect(value).toBe(42);
      expect(unit).toBe('SECONDS');
      expect(checked).toBe(false);
    };

    const wrapper = mount(<TimeUnitInput update={onUpdate} defaultValue={42} defaultEnabled />);
    const checkbox = wrapper.find('input[type="checkbox"]');
    expect(checkbox.prop('checked')).toBe(true);
    expect(wrapper.find('input[type="number"]').prop('value')).toBe(42);
    checkbox.simulate('click');
  });

  it('should use values before default values', () => {
    const onUpdate = (value, unit, checked) => {
      expect(value).toBe(124);
      expect(unit).toBe('SECONDS');
      expect(checked).toBe(true);
    };

    const wrapper = mount(<TimeUnitInput update={onUpdate} value={124} defaultValue={42} enabled={false} defaultEnabled />);
    const checkbox = wrapper.find('input[type="checkbox"]');
    expect(checkbox.prop('checked')).toBe(false);
    expect(wrapper.find('input[type="number"]').prop('value')).toBe(124);
    checkbox.simulate('click');
  });

  it('should use required before enabled and default enabled', () => {
    const onUpdate = (value, unit, checked) => {
      expect(value).toBe(42);
      expect(unit).toBe('SECONDS');
      expect(checked).toBe(true);
    };

    const wrapper = mount(<TimeUnitInput update={onUpdate} required enabled={false} defaultEnabled={false} />);
    expect(wrapper.find('input[type="checkbox"]').length).toBe(0);
    wrapper.find('input[type="number"]').simulate('change', { target: { value: 42 } });
  });
});
