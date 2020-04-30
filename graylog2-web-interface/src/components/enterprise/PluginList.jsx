import React from 'react';
import createReactClass from 'create-react-class';

import { Icon } from 'components/common';
import { PluginStore } from 'graylog-web-plugin/plugin';
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
