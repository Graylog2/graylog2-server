// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { ButtonToolbar, Button } from 'components/graylog';

type Props = {
  activeBackend: ?AuthenticationBackend,
  finishedLoading: boolean,
};

const BackendOverviewLinks = ({ activeBackend, finishedLoading }: Props) => (
  <ButtonToolbar className="pull-right">
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE}>
      <Button bsStyle="info" disabled={!finishedLoading || !activeBackend}>
        Active Service
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW}>
      <Button bsStyle="info">
        All Services
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default BackendOverviewLinks;
