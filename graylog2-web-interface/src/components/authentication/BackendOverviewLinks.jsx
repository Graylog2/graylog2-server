// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

const BackendOverviewLinks = () => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.OVERVIEW}>
      <Button bsStyle="info">
        View Active
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default BackendOverviewLinks;
