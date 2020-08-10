// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

const UserOverviewLinks = () => {
  const teamsRoute = Routes.getPluginRoute('SYSTEM_TEAMS');

  return (
    <ButtonToolbar>
      <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
        <Button bsStyle="info">
          Users Overview
        </Button>
      </LinkContainer>
      {teamsRoute && (
        <LinkContainer to={teamsRoute}>
          <Button bsStyle="info">Teams Overview</Button>
        </LinkContainer>
      )}
    </ButtonToolbar>
  );
};

export default UserOverviewLinks;
