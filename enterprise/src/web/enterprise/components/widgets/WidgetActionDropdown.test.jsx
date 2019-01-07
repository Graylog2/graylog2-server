import React from 'react';
import { mount } from 'enzyme';

import { MenuItem } from 'react-bootstrap';

import WidgetActionDropdown from './WidgetActionDropdown';

describe('WidgetActionDropdown', () => {
  it('opens menu when trigger element is clicked', () => {
    const wrapper = mount((
      <WidgetActionDropdown element={<div className="my-trigger-element">Trigger!</div>}>
        <MenuItem>Foo</MenuItem>
      </WidgetActionDropdown>
    ));
    expect(wrapper).not.toContainMatchingElement('ul.dropdown-menu');
    const trigger = wrapper.find('span[role="presentation"]');
    expect(trigger).toContainMatchingElement('div.my-trigger-element');
    trigger.simulate('click');
    expect(wrapper).toContainMatchingElement('ul.dropdown-menu');
  });
  it('closes menu when MenuItem is clicked', () => {
    const onSelect = jest.fn();
    const wrapper = mount((
      <WidgetActionDropdown element={<div>Trigger!</div>}>
        <MenuItem onSelect={onSelect}>Foo</MenuItem>
      </WidgetActionDropdown>
    ));
    const trigger = wrapper.find('span[role="presentation"]');
    trigger.simulate('click');

    const menuItem = wrapper.find('a[children="Foo"]');
    menuItem.simulate('click');

    expect(onSelect).toHaveBeenCalled();
    expect(wrapper).not.toContainMatchingElement('ul.dropdown-menu');
  });
});
