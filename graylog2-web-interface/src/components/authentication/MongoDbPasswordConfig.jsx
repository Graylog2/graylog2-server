import React, { PropTypes } from 'react';
import { DocumentTitle, PageHeader } from 'components/common';

const MongoDbPasswordConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (
      <DocumentTitle title="Password Authenticator">
        <span>
          <PageHeader title="Password Authenticator" subpage>
            <span>This authenticator uses the password stored in MongoDB to grant access to users, it usually runs last, so that other authentication sources have priority.</span>
          </PageHeader>
          <span>If you only rely on external authentication systems, such as LDAP or Active Directory, you can disable this authenticator. It currently has no configuration options.</span>
        </span>
      </DocumentTitle>
    );
  },
});

export default MongoDbPasswordConfig;
