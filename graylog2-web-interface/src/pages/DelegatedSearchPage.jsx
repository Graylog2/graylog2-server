import React from 'react';

import SearchPage from './SearchPage';
import { PluginStore } from 'graylog-web-plugin/plugin';

const DelegatedSearchPage = (props) => {
  const components = PluginStore.exports("pages")
    .map(c => c.search || {})
    .map(c => c.component)
    .filter(c => c) || [];
  const Component = components[0] || SearchPage;
  return <Component {...props} />;
};

export default DelegatedSearchPage;
