import React from 'react';
import { DocumentTitle, PageHeader } from 'components/common';

const RootUserConfig = () => {
  return (
    <DocumentTitle title="Admin User Authenticator">
      <span>
        <PageHeader title="Admin User Authenticator" subpage>
          <span>This authenticator grants access to the admin user specified in the configuration file.</span>
        </PageHeader>
        <span>Currently the admin user authenticator cannot be configured outside of the configuration file. It can also not be disabled at the moment.</span>
      </span>
    </DocumentTitle>
  );
};

export default RootUserConfig;
