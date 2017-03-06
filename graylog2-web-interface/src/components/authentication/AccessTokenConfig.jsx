import React from 'react';
import { DocumentTitle, PageHeader } from 'components/common';

const AccessTokenConfig = () => {
  return (
    <DocumentTitle title="Access Token Authenticator">
      <span>
        <PageHeader title="Access Token Authenticator" subpage>
          <span>Each user can generate access tokens to avoid having to use their main password in insecure scripts.</span>
        </PageHeader>
        <span>There currently is no configuration available for access tokens. You can safely disable this authenticator if you do not use access tokens.</span>
      </span>
    </DocumentTitle>
  );
};

export default AccessTokenConfig;
