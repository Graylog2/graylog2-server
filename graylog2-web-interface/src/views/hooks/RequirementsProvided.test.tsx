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
import { mount } from 'wrappedEnzyme';
import { PluginStore } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import type { PluginMetadata } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import mockDispatch from 'views/test/mockDispatch';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

import RequirementsProvided from './RequirementsProvided';

import Search from '../logic/search/Search';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

jest.mock('views/Constants', () => ({
  viewsPath: '/path/to/views',
}));

const retry = () => Promise.resolve();

describe('RequirementsProvided', () => {
  const plugin: PluginMetadata = {
    name: 'Pandora\'s Box',
    url: 'https://www.graylog.org',
  };

  const dispatch = mockDispatch();
  const defaultArgs = {
    retry,
    query: {},
    executionState: SearchExecutionState.empty(),
    dispatch,
  };

  it('returns resolved promise for empty requirements', async () => {
    asMock(PluginStore.exports).mockReturnValue([]);

    const view = View.create().toBuilder().requires({}).build();

    expect(await RequirementsProvided({ ...defaultArgs, view })).toBeTruthy();
  });

  it('returns resolved promise if all requirements are provided', async () => {
    asMock(PluginStore.exports).mockReturnValue(['parameters', 'timetravel', 'hyperspeed']);

    const view = View.create()
      .toBuilder()
      .requires({
        parameters: plugin,
        timetravel: plugin,
        hyperspeed: plugin,
      })
      .build();

    expect(await RequirementsProvided({ ...defaultArgs, view })).toBeTruthy();
  });

  it('throws Component if not all requirements are provided', async () => {
    asMock(PluginStore.exports).mockReturnValue(['parameters']);

    const view = View.create()
      .toBuilder()
      .createdAt(new Date('2019-05-29T11:24:51.555Z'))
      .search(
        Search.create()
          .toBuilder()
          .id('5cee6c03675ef9df8b0a7bb0')
          .build(),
      )
      .requires({
        parameters: plugin,
        timetravel: plugin,
        hyperspeed: plugin,
      })
      .build();

    let Component;

    try {
      await RequirementsProvided({ ...defaultArgs, view });
    } catch (component) {
      Component = component;
    }

    expect(mount(<Component />)).toExist();
  });
});
