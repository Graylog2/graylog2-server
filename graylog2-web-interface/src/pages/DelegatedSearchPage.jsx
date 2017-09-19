import React from 'react';

import { PluginStore } from 'graylog-web-plugin/plugin';
import SearchPage from './SearchPage';

export default (props) => {
  const components = PluginStore.exports('pages')
    .map(c => c.search || {})
    .map(c => c.component)
    .filter(c => c) || [];
  const Component = components[0] || SearchPage;
  return <Component {...props} />;
};
