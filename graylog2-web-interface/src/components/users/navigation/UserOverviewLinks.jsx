// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { IfPermitted } from 'components/common';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

const UserOverviewLinks = () => {
  const teamsRoute = Routes.getPluginRoute('SYSTEM_TEAMS');

  return (
    <ButtonToolbar>
      <IfPermitted permissions="users:list">
        <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
          <Button bsStyle="info">
            Users Overview
          </Button>
        </LinkContainer>
      </IfPermitted>
      {teamsRoute && (
        <IfPermitted permissions="teams:list">
          <LinkContainer to={teamsRoute}>
            <Button bsStyle="info">Teams Overview</Button>
          </LinkContainer>
        </IfPermitted>
      )}
    </ButtonToolbar>
  );
};

export default UserOverviewLinks;
