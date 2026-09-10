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
import { render, screen } from 'wrappedTestingLibrary';

import { ScratchpadProvider } from 'contexts/ScratchpadProvider';
import HotkeysProvider from 'contexts/HotkeysProvider';

import ScratchpadToggle from './ScratchpadToggle';

describe('ScratchpadToggle', () => {
  // The shared button hides its overflow, which clips the state indicator drawn just below the icon.
  it('lets the state indicator show outside the button', async () => {
    render(
      <HotkeysProvider>
        <ScratchpadProvider loginName="someone">
          <ScratchpadToggle />
        </ScratchpadProvider>
      </HotkeysProvider>,
    );

    const toggle = await screen.findByRole('button', { name: 'Scratchpad' });

    expect(toggle).toHaveStyleRule('overflow', 'visible', { modifier: '.mantine-Button-label' });
  });
});
