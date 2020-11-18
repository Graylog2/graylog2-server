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
import SelectComponent, { components as Components } from 'react-select';

import Select from './Select';

describe('Select', () => {
  describe('Upgrade to react-select v2', () => {
    const options = [{ label: 'label', value: 'value' }];
    const onChange = () => { };

    it('should convert multi to isMulti', () => {
      const multiWrapper = mount(<Select multi options={options} onChange={onChange} />);

      expect(multiWrapper.find('Select').last().props().isMulti).toBeTruthy();

      const nonMultiWrapper = mount(<Select options={options} onChange={onChange} />);

      expect(nonMultiWrapper.find('Select').last().props().isMulti).toBeFalsy();
    });

    it('should convert disabled to isDisabled', () => {
      const disabledWrapper = mount(<Select disabled options={options} onChange={onChange} />);

      expect(disabledWrapper.find('Select').last().props().isDisabled).toBeTruthy();

      const enabledWrapper = mount(<Select options={options} onChange={onChange} />);

      expect(enabledWrapper.find('Select').last().props().isDisabled).toBeFalsy();
    });

    it('should convert clearable to isClearable', () => {
      const clearableWrapper = mount(<Select options={options} onChange={onChange} />);

      expect(clearableWrapper.find('Select').last().props().isClearable).toBeTruthy();

      const nonClearableWrapper = mount(<Select clearable={false} options={options} onChange={onChange} />);

      expect(nonClearableWrapper.find('Select').last().props().isClearable).toBeFalsy();
    });

    it('should use displayKey to select the option label', () => {
      const customOptions = [{ customLabel: 'my great label', value: 'value' }];
      const wrapper = mount(<Select options={customOptions} onChange={onChange} displayKey="customLabel" menuIsOpen />);

      expect(wrapper.find(Components.Option).props().children).toBe('my great label');
    });

    it('should use valueKey to select the option value', () => {
      const customOptions = [{ label: 'label', customValue: 42 }];
      const wrapper = mount(<Select options={customOptions} onChange={onChange} valueKey="customValue" menuIsOpen />);

      expect(wrapper.find(Components.Option).props().value).toBe(42);
    });

    it('should use matchProp to configure how options are filtered', () => {
      const matchAnyWrapper = mount(<Select options={options} onChange={onChange} />);
      const matchAnyFilter = matchAnyWrapper.find(SelectComponent).props().filterOption;

      expect(matchAnyFilter(options[0], 'label')).toBeTruthy();
      expect(matchAnyFilter(options[0], 'value')).toBeTruthy();

      const matchLabelWrapper = mount(<Select options={options} onChange={onChange} matchProp="label" />);
      const matchLabelFilter = matchLabelWrapper.find(SelectComponent).props().filterOption;

      expect(matchLabelFilter(options[0], 'label')).toBeTruthy();
      expect(matchLabelFilter(options[0], 'value')).toBeFalsy();

      const matchValueWrapper = mount(<Select options={options} onChange={onChange} matchProp="value" />);
      const matchValueFilter = matchValueWrapper.find(SelectComponent).props().filterOption;

      expect(matchValueFilter(options[0], 'label')).toBeFalsy();
      expect(matchValueFilter(options[0], 'value')).toBeTruthy();
    });

    it('should use optionRenderer to customize options\' appearance', () => {
      const optionRenderer = (option) => `Custom ${option.label}`;
      const wrapper = mount(<Select options={options} onChange={onChange} optionRenderer={optionRenderer} menuIsOpen />);

      expect(wrapper.find(Components.Option).props().children).toBe('Custom label');
    });

    it('should use valueRenderer to customize selected value\'s appearance', () => {
      const valueRenderer = (option) => `Custom ${option.value}`;
      const wrapper = mount(<Select options={options} onChange={onChange} valueRenderer={valueRenderer} value={options[0].value} />);

      expect(wrapper.find(Components.SingleValue).props().children).toBe('Custom value');
    });

    it('should disable options that include a disabled property', () => {
      const customOptions = [
        { label: 'enabled', value: 'enabled' },
        { label: 'disabled', value: 'disabled', disabled: true },
      ];
      const wrapper = mount(<Select options={customOptions} onChange={onChange} menuIsOpen />);
      const renderedOptions = wrapper.find(Components.Option);

      expect(renderedOptions).toHaveLength(2);
      expect(renderedOptions.at(0).props().isDisabled).toBeFalsy();
      expect(renderedOptions.at(1).props().isDisabled).toBeTruthy();
    });

    it('should add custom props to input using inputProps', () => {
      const inputProps = { id: 'myId' };
      const wrapper = mount(<Select options={options} onChange={onChange} menuIsOpen inputProps={inputProps} />);

      expect(wrapper.find(Components.Input).props().id).toBe(inputProps.id);
    });
  });
});
