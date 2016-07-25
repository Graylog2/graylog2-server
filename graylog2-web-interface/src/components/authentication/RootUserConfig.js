import React, { PropTypes } from 'react';
import { PageHeader } from 'components/common';

const RootUserConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (<span>
      <PageHeader title="Admin User Authenticator" subpage>
        <span>This authenticator grants access to the admin user specified in the configuration file.</span>
      </PageHeader>
      <span>Currently the admin user authenticator cannot be configured outside of the configuration file. It can also not be disabled at the moment.</span>
    </span>);
  },
});

export default RootUserConfig;
