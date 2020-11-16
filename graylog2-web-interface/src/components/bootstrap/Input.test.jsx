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

import { Button } from 'components/graylog';

import Input from './Input';

describe('Input', () => {
  it('renders a button after the input if buttonAfter is passed', () => {
    const wrapper = mount(<Input id="inputWithButton" type="text" buttonAfter={<Button />} />);

    expect(wrapper.find('button')).toExist();
  });

  it('renders a addon after the input if addonAfter is passed', () => {
    const wrapper = mount(<Input id="inputWithAddon" type="text" addonAfter=".00" />);
    const addon = wrapper.find('span.input-group-addon');

    expect(addon).toExist();
    expect(addon).toHaveText('.00');
  });

  it('renders a checkbox addon after the input if addonAfter is passed', () => {
    const wrapper = mount(<Input id="inputWithCheckboxAddon" type="text" addonAfter={<input id="addonCheckbox" type="checkbox" aria-label="..." />} />);

    expect(wrapper.find('input#addonCheckbox')).toExist();
  });

  it('renders input w/ `name` attribute w/o setting prop', () => {
    const wrapper = mount(<Input id="inputWithoutNameProp" type="text" />);

    expect(wrapper.find('input[name="inputWithoutNameProp"]')).toExist();
  });

  it('renders input w/ `name` attribute w/ setting prop', () => {
    const wrapper = mount(
      <Input id="inputWithoutNameProp" name="inputWithNameProp" type="text" />,
    );

    expect(wrapper.find('input[name="inputWithNameProp"]')).toExist();
  });

  it('renders input w/ provided error', () => {
    const wrapper = mount(<Input id="inputWithError" type="text" error="The error message" />);

    expect(wrapper.find({ error: 'The error message' }).find('InputDescription').length).toEqual(1);
  });

  it('renders input w/ provided help', () => {
    const wrapper = mount(<Input id="inputWithHelp" type="text" help="The help text" />);

    expect(wrapper.find({ help: 'The help text' }).find('InputDescription').length).toEqual(1);
  });

  it('renders input w/ provided help and error', () => {
    const wrapper = mount(<Input id="inputWithHelp" type="text" help="The help text" error="The error message" />);

    expect(wrapper.find({ help: 'The help text', error: 'The error message' }).find('InputDescription').length).toEqual(1);
  });
});
