// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

const BackendOverviewLinks = () => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.ACTIVE}>
      <Button bsStyle="info"
              pullRight>
        Active Service
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.OVERVIEW}>
      <Button bsStyle="info"
              pullRight>
        All Services
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default BackendOverviewLinks;
