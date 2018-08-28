import React from 'react';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import PortaledPopover from './PortaledPopover';

const popoverComponent = mockComponent('PopoverComponent');

describe('PortaledPopover', () => {
  it('does not render popover element when not clicked', () => {
    const wrapper = mount(<PortaledPopover popover={popoverComponent} title="A title">click me!</PortaledPopover>, { attachTo: document.body });

    expect(global.document.body.childNodes).toHaveLength(1);

    wrapper.detach();
  });

  it('renders popover element after being clicked', () => {
    const wrapper = mount(<PortaledPopover popover={popoverComponent} title="A title">click me!</PortaledPopover>, { attachTo: document.body });

    wrapper.find('a').simulate('click');

    expect(global.document.body.childNodes).toHaveLength(2);

    wrapper.detach();
  });
});