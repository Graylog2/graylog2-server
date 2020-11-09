// @flow strict
import * as React from 'react';

import Routes from 'routing/Routes';
import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Button } from 'components/graylog';

const AuthenticatorActionLinks = () => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.AUTHENTICATORS.EDIT}>
      <Button bsStyle="success"
              type="button">
        Edit Authenticators
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);
export default AuthenticatorActionLinks;
