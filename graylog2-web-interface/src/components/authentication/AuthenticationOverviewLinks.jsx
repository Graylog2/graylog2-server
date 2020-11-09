// @flow strict
import * as React from 'react';

import Routes from 'routing/Routes';
import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Button } from 'components/graylog';

const AuthenticationOverviewLinks = () => (
  <ButtonToolbar className="pull-right">
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW}>
      <Button bsStyle="info">
        Authentication Services
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.AUTHENTICATORS.SHOW}>
      <Button bsStyle="info"
              type="button">
        Authenticators
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default AuthenticationOverviewLinks;
