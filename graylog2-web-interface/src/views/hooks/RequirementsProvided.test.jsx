// @flow strict
import type { PluginMetadata } from 'views/logic/views/View';
import * as React from 'react';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';

import { PluginStore } from 'graylog-web-plugin/plugin';
import View from 'views/logic/views/View';
import Search from '../logic/search/Search';

import RequirementsProvided from './RequirementsProvided';

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
  it('returns resolved promise for empty requirements', () => {
    PluginStore.exports.mockReturnValue([]);

    const view = View.create().toBuilder().requires({}).build();

    return RequirementsProvided({ view, query: {}, retry });
  });
  it('returns resolved promise if all requirements are provided', () => {
    PluginStore.exports.mockReturnValue(['parameters', 'timetravel', 'hyperspeed']);

    const view = View.create()
      .toBuilder()
      .requires({
        parameters: plugin,
        timetravel: plugin,
        hyperspeed: plugin,
      })
      .build();

    return RequirementsProvided({ view, query: {}, retry });
  });
  it('throws Component if not all requirements are provided', (done) => {
    PluginStore.exports.mockReturnValue(['parameters']);

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

    RequirementsProvided({ view, query: {}, retry })
      .catch((Component) => {
        const wrapper = mount(<Component />);
        expect(wrapper).toMatchSnapshot();
        done();
      });
  });
});
