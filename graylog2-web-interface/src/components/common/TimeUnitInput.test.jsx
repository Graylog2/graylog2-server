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
    expect(wrapper.find('li.active a').prop('children')).toBe('seconds');

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

  it('should use custom unit values', () => {
    const onUpdate = (value, unit, checked) => {
      expect(value).toBe(42);
      expect(unit).toBe('DAYS');
      expect(checked).toBeTruthy();
    };

    const wrapper = mount(<TimeUnitInput update={onUpdate} unit="DAYS" defaultEnabled />);
    const checkbox = wrapper.find('input[type="checkbox"]');

    expect(checkbox.prop('checked')).toBe(true);
    expect(wrapper.find('input[type="number"]').prop('value')).toBe(1);
    expect(wrapper.find('li.active a').prop('children')).toBe('days');

    wrapper.find('input[type="number"]').simulate('change', { target: { value: 42, type: 'number' } });
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

    wrapper.find('input[type="number"]').simulate('change', { target: { value: 42, type: 'number' } });
  });

  it('should disable all inputs when disabled', () => {
    const wrapper = mount(<TimeUnitInput update={() => {}} enabled={false} />);

    expect(wrapper.find('input[type="number"]').getDOMNode().disabled).toBeTruthy();
    expect(wrapper.find('button.dropdown-toggle').getDOMNode().disabled).toBeTruthy();
  });

  it('should not display checkbox when hideCheckbox is set', () => {
    const wrapper = mount(<TimeUnitInput update={() => {}} hideCheckbox />);

    expect(wrapper.find('input[type="checkbox"]').length).toBe(0);
  });

  it('should use required and enabled when hideCheckbox is set', () => {
    let wrapper = mount(<TimeUnitInput update={() => {}} required enabled={false} defaultEnabled={false} hideCheckbox />);

    expect(wrapper.find('input[type="checkbox"]').length).toBe(0);
    expect(wrapper.find('input[type="number"]').getDOMNode().disabled).toBeFalsy();
    expect(wrapper.find('button.dropdown-toggle').getDOMNode().disabled).toBeFalsy();

    wrapper = mount(<TimeUnitInput update={() => {}} enabled={false} defaultEnabled={false} hideCheckbox />);

    expect(wrapper.find('input[type="checkbox"]').length).toBe(0);
    expect(wrapper.find('input[type="number"]').getDOMNode().disabled).toBeTruthy();
    expect(wrapper.find('button.dropdown-toggle').getDOMNode().disabled).toBeTruthy();
  });

  it('should use default value when clearing the input', () => {
    const handleUpdate = (value, unit, checked) => {
      expect(value).toBe(42);
      expect(unit).toBe('SECONDS');
      expect(checked).toBe(true);
    };

    const wrapper = mount(<TimeUnitInput update={handleUpdate} defaultEnabled value={9} defaultValue={42} />);

    wrapper.find('input[type="number"]').simulate('change', { target: { value: '', type: 'number' } });
  });

  it('should use default value when input receives some text', () => {
    const handleUpdate = (value, unit, checked) => {
      expect(value).toBe(42);
      expect(unit).toBe('SECONDS');
      expect(checked).toBe(true);
    };

    const wrapper = mount(<TimeUnitInput update={handleUpdate} defaultEnabled value={9} defaultValue={42} />);

    wrapper.find('input[type="number"]').simulate('change', { target: { value: 'adsasd', type: 'number' } });
  });

  describe('when clearable is set', () => {
    it('should use undefined when clearing input', () => {
      const handleUpdate = (value, unit, checked) => {
        expect(value).toBe(undefined);
        expect(unit).toBe('SECONDS');
        expect(checked).toBe(true);
      };

      const wrapper = mount(
        <TimeUnitInput update={handleUpdate} defaultEnabled clearable value={9} defaultValue={42} />,
      );

      wrapper.find('input[type="number"]').simulate('change', { target: { value: '', type: 'number' } });
    });

    it('should use undefined when input receives some text', () => {
      const handleUpdate = (value, unit, checked) => {
        expect(value).toBe(undefined);
        expect(unit).toBe('SECONDS');
        expect(checked).toBe(true);
      };

      const wrapper = mount(
        <TimeUnitInput update={handleUpdate} defaultEnabled clearable value={9} defaultValue={42} />,
      );

      wrapper.find('input[type="number"]').simulate('change', { target: { value: 'adsasd', type: 'number' } });
    });

    it('should render empty string when value is undefined', () => {
      const wrapper = mount(
        <TimeUnitInput update={() => {}} defaultEnabled clearable value={undefined} defaultValue={42} />,
      );

      expect(wrapper.find('input[type="number"]').prop('value')).toBe('');
    });
  });
});
