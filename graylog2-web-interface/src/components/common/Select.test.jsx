import React from 'react';
import { mount, shallow } from 'enzyme';

import SelectComponent, { components as Components } from 'react-select';
import Select from './Select';

describe('Select', () => {
  describe('Upgrade to react-select v2', () => {
    const options = [{ label: 'label', value: 'value' }];
    const onChange = () => { };

    it('should convert multi to isMulti', () => {
      const wrapper = shallow(<Select multi options={options} onChange={onChange} />);
      expect(wrapper.props().isMulti).toBeTruthy();
    });

    it('should convert disabled to isDisabled', () => {
      const wrapper = shallow(<Select disabled options={options} onChange={onChange} />);
      expect(wrapper.props().isDisabled).toBeTruthy();
    });

    it('should convert clearable to isClearable', () => {
      const wrapper = shallow(<Select clearable options={options} onChange={onChange} />);
      expect(wrapper.props().isClearable).toBeTruthy();
    });

    it('should use displayKey to select the option label', () => {
      const customOptions = [{ customLabel: 'my great label', value: 'value' }];
      const wrapper = mount(<Select options={customOptions} onChange={onChange} displayKey="customLabel" menuIsOpen />);
      expect(wrapper.find(Components.Option).props().label).toBe('my great label');
    });

    it('should use valueKey to select the option value', () => {
      const customOptions = [{ label: 'label', customValue: 42 }];
      const wrapper = mount(<Select options={customOptions} onChange={onChange} valueKey="customValue" menuIsOpen />);
      expect(wrapper.find(Components.Option).props().value).toBe(42);
    });
  });
});
