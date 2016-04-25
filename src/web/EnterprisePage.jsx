import React from 'react';
import { PageHeader } from 'components/common';
import PluginList from './PluginList';

const EnterprisePage = React.createClass({
  render() {
    return (
      <div>
        <PageHeader title="Graylog Enterprise">
          <span>Graylog Enterprise plugins.</span>
        </PageHeader>

        <PluginList/>
      </div>
    );
  },
});

export default EnterprisePage;
