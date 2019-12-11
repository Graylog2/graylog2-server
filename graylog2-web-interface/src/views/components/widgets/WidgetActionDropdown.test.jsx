// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import { MenuItem } from 'components/graylog';

import WidgetActionDropdown from './WidgetActionDropdown';

describe('WidgetActionDropdown', () => {
  it('opens menu when trigger element is clicked', () => {
    const wrapper = mount((
      <WidgetActionDropdown>
        <MenuItem>Foo</MenuItem>
      </WidgetActionDropdown>
    ));
    expect(wrapper).not.toContainMatchingElement('ul.dropdown-menu');
    const trigger = wrapper.find('ActionToggle');
    trigger.simulate('click');
    expect(wrapper).toContainMatchingElement('ul.dropdown-menu');
  });
  it('closes menu when MenuItem is clicked', () => {
    const onSelect = jest.fn();
    const wrapper = mount((
      <WidgetActionDropdown>
        <MenuItem onSelect={onSelect}>Foo</MenuItem>
      </WidgetActionDropdown>
    ));
    const trigger = wrapper.find('ActionToggle');
    trigger.simulate('click');

    const menuItem = wrapper.find('a[children="Foo"]');
    menuItem.simulate('click');

    expect(onSelect).toHaveBeenCalled();
    expect(wrapper).not.toContainMatchingElement('ul.dropdown-menu');
  });
});
