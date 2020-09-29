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
