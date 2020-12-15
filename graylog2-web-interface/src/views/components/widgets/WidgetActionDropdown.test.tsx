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
