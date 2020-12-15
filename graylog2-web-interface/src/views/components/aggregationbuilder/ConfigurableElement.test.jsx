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
import { mount, shallow } from 'wrappedEnzyme';

import ConfigurableElement from './ConfigurableElement';

describe('ConfigurableElement', () => {
  it('renders something for minimal props', () => {
    const onChange = jest.fn();
    const wrapper = mount(
      <ConfigurableElement title="Configuring Something"
                           onChange={onChange}
                           value={{ label: 42, value: 42 }}
                           configuration={() => <span>A configuration dialog</span>}>
        Hello World!
      </ConfigurableElement>,
    );

    expect(wrapper).toHaveText('Hello World!');
  });

  it('renders the given value', () => {
    const onChange = jest.fn();
    const wrapper = mount(
      <ConfigurableElement title="Configuring Something"
                           onChange={onChange}
                           value={{ label: 42, value: 42 }}
                           configuration={() => <span>A configuration dialog</span>}>
        42
      </ConfigurableElement>,
    );

    expect(wrapper).toIncludeText('42');
  });

  it('value is rendered as a link', () => {
    const onChange = jest.fn();
    const wrapper = mount(
      <ConfigurableElement title="Configuring Something"
                           onChange={onChange}
                           value={{ label: 42, value: 42 }}
                           configuration={() => <span>A configuration dialog</span>}>
        42
      </ConfigurableElement>,
    );
    const link = wrapper.find('.labelAsLink');

    expect(link).toIncludeText('42');
  });

  it('clicking value opens popover', () => {
    const onChange = jest.fn();
    const element = (
      <ConfigurableElement title="Configuring Something"
                           onChange={onChange}
                           value={{ label: 42, value: 42 }}
                           configuration={() => <span>A configuration dialog</span>}>
        42
      </ConfigurableElement>
    );
    const wrapper = shallow(element, { attachTo: document.body });

    expect(wrapper).not.toContain('Popover');
    expect(wrapper).not.toContainReact(<span>A configuration dialog</span>);

    const link = wrapper.find('.labelAsLink');

    link.simulate('click');

    const popover = wrapper.find('Popover');

    expect(popover).toHaveLength(1);
    expect(popover).toHaveProp('title', 'Configuring Something');

    link.simulate('click');

    expect(wrapper.find('Popover')).toHaveLength(0);
  });

  it('submitting configuration dialog calls onChange', () => {
    const onChange = jest.fn();
    const element = (
      <ConfigurableElement title="Configuring Something"
                           onChange={onChange}
                           value={{ label: 42, value: 42 }}
                           configuration={() => <span>A configuration dialog</span>}>
        42
      </ConfigurableElement>
    );
    const wrapper = shallow(element, { attachTo: document.body });

    expect(wrapper).not.toContain('Popover');
    expect(wrapper).not.toContainReact(<span>A configuration dialog</span>);

    const link = wrapper.find('.labelAsLink');

    link.simulate('click');

    const configuration = wrapper.find('configuration');

    expect(configuration).toHaveLength(1);

    configuration.prop('onClose')({ value: 42 });

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith({ value: 42 });
  });
});
