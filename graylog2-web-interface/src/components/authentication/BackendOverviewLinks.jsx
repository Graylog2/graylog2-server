// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

const BackendOverviewLinks = () => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.ACTIVE}>
      <Button pullRight
              bsStyle="info">
        Active Service
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.OVERVIEW}>
      <Button pullRight
              bsStyle="info">
        All Services
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);
export default BackendOverviewLinks;
