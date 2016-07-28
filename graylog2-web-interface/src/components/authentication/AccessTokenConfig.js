import React, { PropTypes } from 'react';
import { PageHeader } from 'components/common';

const AccessTokenConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (<span>
      <PageHeader title="Access Token Authenticator" subpage>
        <span>Each user can generate access tokens to avoid having to use their main password in insecure scripts.</span>
      </PageHeader>
      <span>There currently is no configuration available for access tokens. You can safely disable this authenticator if you do not use access tokens.</span>
    </span>);
  },
});

export default AccessTokenConfig;
