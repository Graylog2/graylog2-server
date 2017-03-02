import React from 'react';
import { DocumentTitle, PageHeader } from 'components/common';

const MongoDbSessionConfig = () => {
  return (
    <DocumentTitle title="Session Authenticator">
      <span>
        <PageHeader title="Session Authenticator" subpage>
          <span>This authenticator uses the session supplied from the web interface to grant access to logged in users, it usually runs first.</span>
        </PageHeader>
        <span>Since sessions are necessary to let the web interface function it cannot be disabled.</span>
      </span>
    </DocumentTitle>
  );
};

export default MongoDbSessionConfig;
