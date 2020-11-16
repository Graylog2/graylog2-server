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
