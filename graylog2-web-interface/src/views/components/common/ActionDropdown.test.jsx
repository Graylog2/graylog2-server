// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import { MenuItem } from 'components/graylog';

import ActionDropdown from './ActionDropdown';

describe('ActionDropdown', () => {
  it('opens menu when trigger element is clicked', () => {
    const wrapper = mount((
      <ActionDropdown element={<div className="my-trigger-element">Trigger!</div>}>
        <MenuItem>Foo</MenuItem>
      </ActionDropdown>
    ));
    expect(wrapper).not.toContainMatchingElement('ul.dropdown-menu');
    const trigger = wrapper.find('ActionToggle');
    expect(trigger).toContainMatchingElement('div.my-trigger-element');
    trigger.simulate('click');
    expect(wrapper).toContainMatchingElement('ul.dropdown-menu');
  });
  it('stops event when trigger element is clicked', () => {
    const onClick = jest.fn();
    const wrapper = mount((
      <div onClick={onClick}>
        <ActionDropdown element={<div className="my-trigger-element">Trigger!</div>}>
          <MenuItem>Foo</MenuItem>
        </ActionDropdown>
      </div>
    ));
    const trigger = wrapper.find('ActionToggle');
    trigger.simulate('click');

    expect(onClick).not.toHaveBeenCalled();
  });
  it('closes menu when MenuItem is clicked', () => {
    const onSelect = jest.fn();
    const wrapper = mount((
      <ActionDropdown element={<div>Trigger!</div>}>
        <MenuItem onSelect={onSelect}>Foo</MenuItem>
      </ActionDropdown>
    ));
    const trigger = wrapper.find('ActionToggle');
    trigger.simulate('click');

    const menuItem = wrapper.find('a[children="Foo"]');
    menuItem.simulate('click');

    expect(onSelect).toHaveBeenCalled();
    expect(wrapper).not.toContainMatchingElement('ul.dropdown-menu');
  });
  it('stops click event when MenuItem is clicked', () => {
    const onClick = jest.fn();
    const onSelect = jest.fn();
    const wrapper = mount((
      <div onClick={onClick}>
        <ActionDropdown element={<div>Trigger!</div>}>
          <MenuItem onSelect={onSelect}>Foo</MenuItem>
        </ActionDropdown>
      </div>
    ));
    const trigger = wrapper.find('ActionToggle');
    trigger.simulate('click');

    const menuItem = wrapper.find('a[children="Foo"]');
    menuItem.simulate('click');

    expect(onClick).not.toHaveBeenCalled();
  });
});
