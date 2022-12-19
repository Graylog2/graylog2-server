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
import { render } from 'wrappedTestingLibrary';
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import PluggableCommands from './PluggableCommands';

describe('PluggableCommands', () => {
  const plugin: PluginExports = {
    'views.queryInput.commands': [{
      name: 'Testcommand',
      bindKey: {
        mac: 'Ctrl+Enter',
        win: 'Ctrl+Enter',
      },
      usages: ['search_query'],
      exec: (editor, context) => ({ editor, context }),
    }],
    'views.queryInput.commandContextProviders': [{
      key: 'foo',
      provider: () => 42,
    }],
  };

  const manifest = new PluginManifest({}, plugin);

  beforeEach(() => {
    PluginStore.register(manifest);
  });

  afterEach(() => {
    PluginStore.unregister(manifest);
  });

  it('retrieves commands from plugins for current scope', () => {
    render((
      <PluggableCommands usage="search_query">
        {(commands) => {
          expect(commands.length).not.toBe(0);

          return null;
        }}
      </PluggableCommands>
    ));
  });

  it('ignores commands from plugins for other scopes', () => {
    render((
      <PluggableCommands usage="global_override_query">
        {(commands) => {
          expect(commands.length).toBe(0);

          return null;
        }}
      </PluggableCommands>
    ));
  });

  it('extends context with values from providers', () => {
    render((
      <PluggableCommands usage="search_query">
        {(commands) => {
          const [command] = commands;
          const inputEditor = {};

          // @ts-ignore
          const { editor, context } = command.exec(inputEditor);

          expect(editor).toBe(inputEditor);
          expect(context).toStrictEqual({ usage: 'search_query', foo: 42 });

          return null;
        }}
      </PluggableCommands>
    ));
  });
});
