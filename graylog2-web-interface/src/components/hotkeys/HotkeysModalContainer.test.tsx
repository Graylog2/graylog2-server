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
import userEvent from '@testing-library/user-event';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import HotkeysModalContainer from 'components/hotkeys/HotkeysModalContainer';
import HotkeysProvider from 'contexts/HotkeysProvider';
import useHotkey from 'hooks/useHotkey';

describe('HotkeysModalContainer', () => {
  it('should render hotkeys modal after pressing keyboard shortcut', async () => {
    render(
      <HotkeysProvider>
        <HotkeysModalContainer />
      </HotkeysProvider>,
    );

    await userEvent.keyboard('{Shift>}?{/Shift}');

    await screen.findByRole('heading', {
      name: /keyboard shortcuts/i,
    });
  });

  it('runs and lists hotkeys added by plugins', async () => {
    const plugin = new PluginManifest(
      {},
      { hotkeys: [{ scope: 'general', actions: { 'open-example': { keys: 'mod+.', description: 'Open example' } } }] },
    );
    PluginStore.register(plugin);
    const onOpenExample = jest.fn();
    const OpenExample = () => {
      useHotkey({ scope: 'general', actionKey: 'open-example', callback: onOpenExample });

      return null;
    };

    render(
      <HotkeysProvider>
        <>
          <OpenExample />
          <HotkeysModalContainer />
        </>
      </HotkeysProvider>,
    );

    await userEvent.keyboard('{Control>}.{/Control}');

    expect(onOpenExample).toHaveBeenCalledTimes(1);

    await userEvent.keyboard('{Shift>}?{/Shift}');

    await screen.findByText('Open example');

    PluginStore.unregister(plugin);
  });
});
