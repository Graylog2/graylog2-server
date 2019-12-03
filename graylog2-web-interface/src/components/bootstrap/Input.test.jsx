import React from 'react';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';

import { Button } from 'components/graylog';
import Input from './Input';

describe('Input', () => {
  it('renders a button after the input if buttonAfter is passed', () => {
    const wrapper = mount(<Input id="inputWithButton" type="text" buttonAfter={<Button />} />);
    expect(wrapper.find('button')).toExist();
    expect(wrapper).toMatchSnapshot();
  });

  it('renders a addon after the input if addonAfter is passed', () => {
    const wrapper = mount(<Input id="inputWithAddon" type="text" addonAfter=".00" />);
    const addon = wrapper.find('span.input-group-addon');
    expect(addon).toExist();
    expect(addon).toHaveText('.00');
    expect(wrapper).toMatchSnapshot();
  });

  it('renders a checkbox addon after the input if addonAfter is passed', () => {
    const wrapper = mount(<Input id="inputWithCheckboxAddon" type="text" addonAfter={<input id="addonCheckbox" type="checkbox" aria-label="..." />} />);
    expect(wrapper.find('input#addonCheckbox')).toExist();
    expect(wrapper).toMatchSnapshot();
  });

  it('renders input w/ `name` attribute w/o setting prop', () => {
    const wrapper = mount(<Input id="inputWithoutNameProp" type="text" />);
    expect(wrapper.find('input[name="inputWithoutNameProp"]')).toExist();
    expect(wrapper).toMatchSnapshot();
  });

  it('renders input w/ `name` attribute w/ setting prop', () => {
    const wrapper = mount(
      <Input id="inputWithoutNameProp" name="inputWithNameProp" type="text" />,
    );
    expect(wrapper.find('input[name="inputWithNameProp"]')).toExist();
    expect(wrapper).toMatchSnapshot();
  });
});
