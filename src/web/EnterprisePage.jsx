import React from 'react';
import { PageHeader } from 'components/common';
import PluginList from './PluginList';

const EnterprisePage = React.createClass({
  render() {
    return (
      <div>
        <PageHeader title="Graylog Enterprise">
          {null}
          
          <span>
            Graylog Enterprise adds commercial functionality to the Open Source Graylog core. You can learn more
            about Graylog Enterprise and order a license on the <a href="https://www.graylog.org/enterprise" target="_blank">product page</a>.
          </span>
          
          <span>
            <a className="btn btn-lg btn-success" href="https://www.graylog.org/enterprise" target="_blank">Order a license</a>
          </span>
        </PageHeader>

        <PluginList/>
      </div>
    );
  },
});

export default EnterprisePage;
