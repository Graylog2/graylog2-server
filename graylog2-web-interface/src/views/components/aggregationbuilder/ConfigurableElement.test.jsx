import React from 'react';
import renderer from 'react-test-renderer';
import { mount, shallow } from 'wrappedEnzyme';

import ConfigurableElement from './ConfigurableElement';

describe('ConfigurableElement', () => {
  it('renders something for minimal props', () => {
    const onChange = jest.fn();
    const wrapper = renderer.create(
      <ConfigurableElement title="Configuring Something"
                           onChange={onChange}
                           value={{ label: 42, value: 42 }}
                           configuration={() => <span>A configuration dialog</span>}>
        Hello World!
      </ConfigurableElement>,
    );
    expect(wrapper.toJSON()).toMatchSnapshot();
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
