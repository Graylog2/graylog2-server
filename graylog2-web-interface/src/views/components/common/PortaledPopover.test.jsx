import React from 'react';
import { mount } from 'wrappedEnzyme';

import PortaledPopover from './PortaledPopover';

const PopoverComponent = () => 'popover-component';

describe('PortaledPopover', () => {
  let appRootContainer;
  let portalsContainer;
  beforeEach(() => {
    appRootContainer = document.createElement('div');
    portalsContainer = document.createElement('div');
    portalsContainer.id = 'portals';
  });
  it('does not render popover element when not clicked', () => {
    const wrapper = mount((
      <PortaledPopover popover={<PopoverComponent />}
                       container={portalsContainer}
                       title="A title">
        click me!
      </PortaledPopover>
    ), { attachTo: appRootContainer });

    expect(appRootContainer.childNodes).toHaveLength(1);
    expect(portalsContainer.childNodes).toHaveLength(0);

    wrapper.detach();
  });

  it('renders popover element after being clicked', () => {
    const wrapper = mount((
      <PortaledPopover popover={<PopoverComponent />}
                       container={portalsContainer}
                       title="A title">
        click me!
      </PortaledPopover>
    ), { attachTo: appRootContainer });

    wrapper.find('a').simulate('click');

    expect(appRootContainer.childNodes).toHaveLength(1);
    expect(portalsContainer.childNodes).toHaveLength(1);
    expect(portalsContainer.childNodes[0].innerHTML).toMatchSnapshot();

    wrapper.detach();
  });
});
