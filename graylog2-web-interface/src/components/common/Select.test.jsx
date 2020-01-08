import React from 'react';
import { shallow } from 'enzyme';

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
  });
});
