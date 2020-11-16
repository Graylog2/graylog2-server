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
import createReactClass from 'create-react-class';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Icon } from 'components/common';

import style from './PluginList.css';

const PluginList = createReactClass({
  displayName: 'PluginList',

  ENTERPRISE_PLUGINS: {
    'graylog-plugin-enterprise': 'Graylog Plugin Enterprise',
  },

  _formatPlugin(pluginName) {
    const plugin = PluginStore.get().filter((p) => p.metadata.name === pluginName)[0];

    return (
      <li key={pluginName} className={plugin ? 'text-success' : 'text-danger'}>
        <Icon name={plugin ? 'check-circle' : 'minus-circle'} />&nbsp;
        {this.ENTERPRISE_PLUGINS[pluginName]} is {plugin ? 'installed' : 'not installed'}
      </li>
    );
  },

  render() {
    const enterprisePluginList = Object.keys(this.ENTERPRISE_PLUGINS).map((pluginName) => this._formatPlugin(pluginName));

    return (
      <>
        <p>This is the status of Graylog Enterprise modules in this cluster:</p>
        <ul className={style.enterprisePlugins}>
          {enterprisePluginList}
        </ul>
      </>
    );
  },
});

export default PluginList;
